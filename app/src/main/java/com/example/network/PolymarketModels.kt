package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TradeOpportunity(
    val id: String,
    val title: String,
    val description: String,
    val endsAt: String,
    val url: String,
    val probability: Int, // e.g. 72
    val delta: Double, // e.g. +4.2 or -1.8
    val confidenceScore: Double, // e.g. 98.4
    val confidenceGrade: String, // e.g. "S", "A", "ULTRA"
    val volume: String, // e.g. "$1.2M"
    val liquidity: String, // e.g. "High", "Med", "Thin"
    val hftSignal: String, // e.g. "DETECTED" or "STABLE"
    val category: String = "Macro Economics",
    // Real Polymarket identifiers for read-only market data (charts, books).
    val tokenId: String? = null, // CLOB ERC-1155 token id of the YES outcome
    val conditionId: String? = null // 0x… market condition id
)

@JsonClass(generateAdapter = true)
data class PolymarketEvent(
    val id: String?,
    val title: String?,
    val description: String?,
    @Json(name = "endDate") val endDate: String?,
    val markets: List<PolymarketMarket>?,
    val slug: String? = null,
    // NOTE: numbers on Event objects (strings on Market objects).
    val volume: Double? = null,
    val liquidity: Double? = null,
    val volume24hr: Double? = null
)

@JsonClass(generateAdapter = true)
data class PolymarketMarket(
    val id: String?,
    val question: String?,
    @Json(name = "conditionId") val conditionId: String?,
    @Json(name = "active") val active: Boolean?,
    @Json(name = "volume") val volume: String?,
    @Json(name = "endDate") val endDate: String?,
    val slug: String? = null,
    // JSON-string-encoded arrays (decode with parseJsonStringArray/DoubleArray)
    val outcomes: String? = null,
    @Json(name = "outcomePrices") val outcomePrices: String? = null,
    @Json(name = "clobTokenIds") val clobTokenIds: String? = null,
    @Json(name = "volumeNum") val volumeNum: Double? = null,
    @Json(name = "liquidityNum") val liquidityNum: Double? = null,
    @Json(name = "oneDayPriceChange") val oneDayPriceChange: Double? = null,
    @Json(name = "bestBid") val bestBid: Double? = null,
    @Json(name = "bestAsk") val bestAsk: Double? = null
)
