package com.polytrader.app.data.remote.ws

import com.polytrader.app.domain.model.BookLevel
import com.polytrader.app.domain.model.MarketWsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Public CLOB market channel
 * (`wss://ws-subscriptions-clob.polymarket.com/ws/market`, no auth).
 *
 * Protocol (docs + AsyncAPI, July 2026):
 *  - first frame: `{"assets_ids":[...],"type":"market"}`
 *  - keepalive: text frame `PING` every 10s (server replies `PONG`)
 *  - events discriminated by `event_type`; a frame may carry a JSON array
 *  - all numerics are strings; prices may lack a leading zero (".48")
 */
class MarketWebSocketClient(
    private val okHttpClient: OkHttpClient,
    private val url: String = "wss://ws-subscriptions-clob.polymarket.com/ws/market",
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var subscribedTokenIds: List<String> = emptyList()
    private var shouldReconnect = false

    private val _events = MutableSharedFlow<MarketWsEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<MarketWsEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** (Re)connects and subscribes to the given CLOB token ids. */
    fun connect(tokenIds: List<String>) {
        if (tokenIds.isEmpty()) return
        subscribedTokenIds = tokenIds
        shouldReconnect = true
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        heartbeatJob?.cancel()
        webSocket?.close(1000, "bye")
        webSocket = null
        _connected.value = false
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }

    private fun openSocket() {
        webSocket?.close(1000, "resubscribe")
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, Listener())
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connected.value = true
            val subscribe = JSONObject().apply {
                put("assets_ids", JSONArray(subscribedTokenIds))
                put("type", "market")
            }
            webSocket.send(subscribe.toString())

            heartbeatJob?.cancel()
            heartbeatJob = scope.launch {
                while (isActive) {
                    delay(10_000)
                    webSocket.send("PING")
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (text == "PONG" || text.isBlank()) return
            try {
                when (text.trimStart().firstOrNull()) {
                    '[' -> {
                        val arr = JSONArray(text)
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.let(::dispatch)
                        }
                    }
                    '{' -> dispatch(JSONObject(text))
                    else -> Unit
                }
            } catch (_: Exception) {
                // malformed frame — ignore
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connected.value = false
            heartbeatJob?.cancel()
            if (shouldReconnect) {
                scope.launch {
                    delay(5_000)
                    if (shouldReconnect) openSocket()
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connected.value = false
            heartbeatJob?.cancel()
        }
    }

    private fun dispatch(json: JSONObject) {
        when (json.optString("event_type")) {
            "book" -> parseBook(json)?.let { _events.tryEmit(it) }
            "price_change" -> parsePriceChanges(json).forEach { _events.tryEmit(it) }
            "last_trade_price" -> parseLastTrade(json)?.let { _events.tryEmit(it) }
        }
    }

    private fun parseBook(json: JSONObject): MarketWsEvent.BookSnapshot? {
        val tokenId = json.optString("asset_id").takeIf { it.isNotBlank() } ?: return null
        fun levels(key: String): List<BookLevel> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val p = lenientDouble(o.optString("price")) ?: return@mapNotNull null
                val s = lenientDouble(o.optString("size")) ?: return@mapNotNull null
                BookLevel(p, s)
            }
        }
        return MarketWsEvent.BookSnapshot(
            tokenId = tokenId,
            bids = levels("bids").sortedByDescending { it.price },
            asks = levels("asks").sortedBy { it.price },
            timestampMs = json.optString("timestamp").toLongOrNull(),
        )
    }

    private fun parsePriceChanges(json: JSONObject): List<MarketWsEvent.PriceChange> {
        val timestamp = json.optString("timestamp").toLongOrNull()
        val changes = json.optJSONArray("price_changes")
            ?: json.optJSONArray("changes") // deprecated flat shape
            ?: return emptyList()
        return (0 until changes.length()).mapNotNull { i ->
            val o = changes.optJSONObject(i) ?: return@mapNotNull null
            val tokenId = o.optString("asset_id")
                .takeIf { it.isNotBlank() } ?: json.optString("asset_id").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            MarketWsEvent.PriceChange(
                tokenId = tokenId,
                price = lenientDouble(o.optString("price")) ?: return@mapNotNull null,
                size = lenientDouble(o.optString("size")) ?: 0.0,
                side = o.optString("side"),
                bestBid = lenientDouble(o.optString("best_bid")),
                bestAsk = lenientDouble(o.optString("best_ask")),
                timestampMs = timestamp,
            )
        }
    }

    private fun parseLastTrade(json: JSONObject): MarketWsEvent.LastTrade? {
        val tokenId = json.optString("asset_id").takeIf { it.isNotBlank() } ?: return null
        return MarketWsEvent.LastTrade(
            tokenId = tokenId,
            price = lenientDouble(json.optString("price")) ?: return null,
            size = lenientDouble(json.optString("size")) ?: 0.0,
            side = json.optString("side"),
            timestampMs = json.optString("timestamp").toLongOrNull(),
        )
    }

    /** Parses ".48" style decimals the channel is known to emit. */
    private fun lenientDouble(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val normalized = if (raw.startsWith(".")) "0$raw" else raw
        return normalized.toDoubleOrNull()
    }
}
