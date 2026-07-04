package com.polytrader.app.data.remote.dataapi

import com.polytrader.app.core.util.Format
import com.polytrader.app.domain.model.ActivityItem
import com.polytrader.app.domain.model.Position
import com.polytrader.app.domain.model.TopHolder
import com.squareup.moshi.JsonClass
import java.time.Instant

/*
 * Data API DTOs (data-api.polymarket.com). Public, no auth. All endpoints
 * expect the user's PROXY WALLET address (the one in polymarket.com/profile
 * URLs), not the signing EOA.
 */

@JsonClass(generateAdapter = true)
data class PositionDto(
    val proxyWallet: String? = null,
    val asset: String? = null,
    val conditionId: String? = null,
    val size: Double? = null,
    val avgPrice: Double? = null,
    val initialValue: Double? = null,
    val currentValue: Double? = null,
    val cashPnl: Double? = null,
    val percentPnl: Double? = null,
    val totalBought: Double? = null,
    val realizedPnl: Double? = null,
    val curPrice: Double? = null,
    val redeemable: Boolean? = null,
    val mergeable: Boolean? = null,
    val title: String? = null,
    val slug: String? = null,
    val icon: String? = null,
    val eventSlug: String? = null,
    val outcome: String? = null,
    val outcomeIndex: Int? = null,
    val endDate: String? = null,
    val negativeRisk: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class ActivityDto(
    val proxyWallet: String? = null,
    val timestamp: Long? = null, // unix seconds
    val conditionId: String? = null,
    val type: String? = null,    // TRADE, SPLIT, MERGE, REDEEM, REWARD, ...
    val size: Double? = null,
    val usdcSize: Double? = null,
    val transactionHash: String? = null,
    val price: Double? = null,
    val asset: String? = null,
    val side: String? = null,
    val outcomeIndex: Int? = null,
    val title: String? = null,
    val slug: String? = null,
    val icon: String? = null,
    val eventSlug: String? = null,
    val outcome: String? = null,
)

@JsonClass(generateAdapter = true)
data class TradeDto(
    val proxyWallet: String? = null,
    val side: String? = null,
    val asset: String? = null,
    val conditionId: String? = null,
    val size: Double? = null,
    val price: Double? = null,
    val timestamp: Long? = null, // unix seconds
    val title: String? = null,
    val slug: String? = null,
    val icon: String? = null,
    val eventSlug: String? = null,
    val outcome: String? = null,
    val outcomeIndex: Int? = null,
    val name: String? = null,
    val pseudonym: String? = null,
    val profileImage: String? = null,
    val transactionHash: String? = null,
)

@JsonClass(generateAdapter = true)
data class ValueDto(
    val user: String? = null,
    val value: Double? = null,
)

@JsonClass(generateAdapter = true)
data class MetaHolderDto(
    val token: String? = null,
    val holders: List<HolderDto>? = null,
)

@JsonClass(generateAdapter = true)
data class HolderDto(
    val proxyWallet: String? = null,
    val pseudonym: String? = null,
    val name: String? = null,
    val amount: Double? = null,
    val asset: String? = null,
    val outcomeIndex: Int? = null,
    val profileImage: String? = null,
)

/* ---------------------------------------------------------------------------
 * Mappers
 * ------------------------------------------------------------------------- */

fun PositionDto.toDomain(): Position? {
    return Position(
        proxyWallet = proxyWallet ?: return null,
        asset = asset ?: return null,
        conditionId = conditionId ?: "",
        title = title ?: "Unknown market",
        slug = slug ?: "",
        eventSlug = eventSlug,
        icon = icon,
        outcome = outcome ?: "",
        outcomeIndex = outcomeIndex ?: 0,
        size = size ?: 0.0,
        avgPrice = avgPrice ?: 0.0,
        curPrice = curPrice ?: 0.0,
        initialValue = initialValue ?: 0.0,
        currentValue = currentValue ?: 0.0,
        cashPnl = cashPnl ?: 0.0,
        percentPnl = percentPnl ?: 0.0,
        realizedPnl = realizedPnl ?: 0.0,
        redeemable = redeemable ?: false,
        endDate = Format.parseInstant(endDate),
        negativeRisk = negativeRisk ?: false,
    )
}

fun ActivityDto.toDomain(): ActivityItem? {
    val ts = timestamp ?: return null
    return ActivityItem(
        timestamp = Instant.ofEpochSecond(ts),
        type = type ?: "TRADE",
        side = side,
        title = title ?: "Unknown market",
        slug = slug,
        eventSlug = eventSlug,
        icon = icon,
        outcome = outcome,
        size = size,
        usdcSize = usdcSize,
        price = price,
        transactionHash = transactionHash,
    )
}

fun TradeDto.toDomain(): com.polytrader.app.domain.model.MarketTrade? {
    val ts = timestamp ?: return null
    return com.polytrader.app.domain.model.MarketTrade(
        timestamp = Instant.ofEpochSecond(ts),
        side = side ?: "BUY",
        price = price ?: return null,
        size = size ?: 0.0,
        outcome = outcome ?: "",
        outcomeIndex = outcomeIndex ?: 0,
        traderName = name?.takeIf { it.isNotBlank() }
            ?: pseudonym?.takeIf { it.isNotBlank() }
            ?: Format.shortAddress(proxyWallet),
        proxyWallet = proxyWallet,
        transactionHash = transactionHash,
    )
}

fun HolderDto.toDomain(): TopHolder? {
    val wallet = proxyWallet ?: return null
    return TopHolder(
        proxyWallet = wallet,
        displayName = name?.takeIf { it.isNotBlank() }
            ?: pseudonym?.takeIf { it.isNotBlank() }
            ?: Format.shortAddress(wallet),
        amount = amount ?: 0.0,
        outcomeIndex = outcomeIndex ?: 0,
        profileImage = profileImage,
    )
}
