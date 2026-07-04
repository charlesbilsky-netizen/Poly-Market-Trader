package com.example.network

import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject

class PolymarketWebSocketClient(
    private val client: OkHttpClient,
    private val onMessageReceived: (String) -> Unit
) : WebSocketListener() {
    private val request = Request.Builder().url("wss://ws-subscriptions-clob.polymarket.com/ws/market").build()
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        webSocket = client.newWebSocket(request, this)
    }

    fun subscribe(tokenIds: List<String>) {
        val jsonArray = org.json.JSONArray()
        tokenIds.forEach { jsonArray.put(it) }
        
        val message = JSONObject().apply {
            put("type", "market")
            put("assets_ids", jsonArray)
            put("custom_feature_enabled", true)
        }
        webSocket?.send(message.toString())
    }

    fun unsubscribe(tokenIds: List<String>) {
        val jsonArray = org.json.JSONArray()
        tokenIds.forEach { jsonArray.put(it) }
        
        val message = JSONObject().apply {
            put("operation", "unsubscribe")
            put("assets_ids", jsonArray)
        }
        webSocket?.send(message.toString())
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(10_000)
                webSocket.send("PING")
            }
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        if (text == "PONG") return
        onMessageReceived(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        heartbeatJob?.cancel()
        // Simple reconnect after failure
        scope.launch {
            delay(5000)
            connect()
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        heartbeatJob?.cancel()
    }

    fun close() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }
}
