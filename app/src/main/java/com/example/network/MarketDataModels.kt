package com.example.network

import org.json.JSONArray

/*
 * Read-only CLOB market-data models (prices-history + order book).
 * All CLOB book prices/sizes arrive as strings; /prices-history points are
 * numbers. Parsed with the reflective Moshi adapter already used by
 * NetworkModule, so these stay plain Kotlin data classes.
 */

data class PriceHistoryResponse(
    val history: List<PriceHistoryPoint>? = null,
)

data class PriceHistoryPoint(
    val t: Long? = null, // unix seconds
    val p: Double? = null, // probability 0..1
)

data class ClobBookLevel(
    val price: String? = null,
    val size: String? = null,
)

data class ClobBookResponse(
    val market: String? = null,
    val asset_id: String? = null,
    val timestamp: String? = null,
    val bids: List<ClobBookLevel>? = null,
    val asks: List<ClobBookLevel>? = null,
    val tick_size: String? = null,
)

/* --- UI-facing snapshots ------------------------------------------------- */

data class HistoryPoint(val timeSec: Long, val price: Double)

data class DepthLevel(val price: Double, val size: Double)

/** Order book normalized best-first for both sides. */
data class BookSnapshot(
    val tokenId: String,
    val bids: List<DepthLevel>,
    val asks: List<DepthLevel>,
) {
    val bestBid: Double? get() = bids.firstOrNull()?.price
    val bestAsk: Double? get() = asks.firstOrNull()?.price
    val spread: Double? get() {
        val b = bestBid ?: return null
        val a = bestAsk ?: return null
        return a - b
    }
    val bidDepthUsd: Double get() = bids.sumOf { it.price * it.size }
    val askDepthUsd: Double get() = asks.sumOf { (1 - it.price) * it.size }
}

fun PriceHistoryResponse.toPoints(): List<HistoryPoint> =
    history.orEmpty().mapNotNull { pt ->
        val t = pt.t ?: return@mapNotNull null
        val p = pt.p ?: return@mapNotNull null
        HistoryPoint(t, p)
    }

/**
 * CLOB books are sorted worst→best (best level LAST); normalize to best-first
 * so `bids[0]` / `asks[0]` are top-of-book.
 */
fun ClobBookResponse.toSnapshot(): BookSnapshot? {
    val token = asset_id ?: return null
    fun List<ClobBookLevel>?.parse(): List<DepthLevel> =
        orEmpty().mapNotNull { level ->
            val p = level.price?.toDoubleOrNull() ?: return@mapNotNull null
            val s = level.size?.toDoubleOrNull() ?: return@mapNotNull null
            DepthLevel(p, s)
        }
    return BookSnapshot(
        tokenId = token,
        bids = bids.parse().sortedByDescending { it.price },
        asks = asks.parse().sortedBy { it.price },
    )
}

/** Decodes Gamma's JSON-string-encoded arrays, e.g. "[\"Yes\", \"No\"]". */
fun parseJsonStringArray(encoded: String?): List<String> {
    if (encoded.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(encoded)
        List(arr.length()) { arr.optString(it, "") }
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseJsonDoubleArray(encoded: String?): List<Double> =
    parseJsonStringArray(encoded).mapNotNull { it.toDoubleOrNull() }
