package com.polytrader.app.data.remote.clob

import com.polytrader.app.domain.model.BookLevel
import com.polytrader.app.domain.model.OrderBook
import com.polytrader.app.domain.model.PricePoint
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * CLOB read-only DTOs (clob.polymarket.com). All prices/sizes arrive as
 * strings; `/prices-history` points are numbers. No auth is required for any
 * of these endpoints — this app deliberately has no access to the
 * order-placement (L1/L2 authed) surface.
 */

@JsonClass(generateAdapter = true)
data class ClobBookLevelDto(
    val price: String?,
    val size: String?,
)

@JsonClass(generateAdapter = true)
data class ClobBookDto(
    val market: String? = null,
    @Json(name = "asset_id") val assetId: String? = null,
    val timestamp: String? = null,
    val bids: List<ClobBookLevelDto>? = null,
    val asks: List<ClobBookLevelDto>? = null,
    @Json(name = "min_order_size") val minOrderSize: String? = null,
    @Json(name = "tick_size") val tickSize: String? = null,
    @Json(name = "neg_risk") val negRisk: Boolean? = null,
    @Json(name = "last_trade_price") val lastTradePrice: String? = null,
)

@JsonClass(generateAdapter = true)
data class ClobMidpointDto(val mid: String? = null)

@JsonClass(generateAdapter = true)
data class ClobSpreadDto(val spread: String? = null)

@JsonClass(generateAdapter = true)
data class ClobPriceDto(val price: String? = null)

@JsonClass(generateAdapter = true)
data class PriceHistoryDto(
    val history: List<PriceHistoryPointDto>? = null,
)

@JsonClass(generateAdapter = true)
data class PriceHistoryPointDto(
    val t: Long?,
    val p: Double?,
)

/* ---------------------------------------------------------------------------
 * Mappers
 * ------------------------------------------------------------------------- */

/**
 * CLOB books are sorted worst→best (best level LAST). Normalize to best-first
 * so the UI can read `bids[0]` / `asks[0]` as top-of-book.
 */
fun ClobBookDto.toDomain(): OrderBook? {
    val tokenId = assetId ?: return null
    fun List<ClobBookLevelDto>?.parse(): List<BookLevel> =
        orEmpty().mapNotNull { level ->
            val p = level.price?.toDoubleOrNull() ?: return@mapNotNull null
            val s = level.size?.toDoubleOrNull() ?: return@mapNotNull null
            BookLevel(p, s)
        }
    return OrderBook(
        tokenId = tokenId,
        bids = bids.parse().sortedByDescending { it.price },
        asks = asks.parse().sortedBy { it.price },
        timestampMs = timestamp?.toLongOrNull(),
    )
}

fun PriceHistoryDto.toDomain(): List<PricePoint> =
    history.orEmpty().mapNotNull { point ->
        val t = point.t ?: return@mapNotNull null
        val p = point.p ?: return@mapNotNull null
        PricePoint(timeSec = t, price = p)
    }
