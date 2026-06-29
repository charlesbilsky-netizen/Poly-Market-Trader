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
    val category: String = "Macro Economics"
)

@JsonClass(generateAdapter = true)
data class PolymarketEvent(
    val id: String?,
    val title: String?,
    val description: String?,
    @Json(name = "endDate") val endDate: String?,
    val markets: List<PolymarketMarket>?,
    val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class PolymarketMarket(
    val id: String?,
    val question: String?,
    @Json(name = "conditionId") val conditionId: String?,
    @Json(name = "active") val active: Boolean?,
    @Json(name = "volume") val volume: String?,
    @Json(name = "endDate") val endDate: String?,
    val slug: String? = null
)

@JsonClass(generateAdapter = true)
data class ClobOrder(
    val signer: String?,
    val maker: String?,
    val taker: String?,
    @Json(name = "tokenId") val tokenId: String?,
    @Json(name = "makerAmount") val makerAmount: String?,
    @Json(name = "takerAmount") val takerAmount: String?,
    val side: Int?, // 0 = buy, 1 = sell
    val expiration: String?,
    val nonce: String?,
    val salt: String?,
    val signature: String?
)

@JsonClass(generateAdapter = true)
data class ClobOrderRequest(
    val order: ClobOrder?,
    val owner: String?
)

@JsonClass(generateAdapter = true)
data class ClobOrderResponse(
    val success: Boolean?,
    val orderHash: String?,
    val errorMsg: String? = null
)
