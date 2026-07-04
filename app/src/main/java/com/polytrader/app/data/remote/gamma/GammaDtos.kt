package com.polytrader.app.data.remote.gamma

import com.polytrader.app.core.util.Format
import com.polytrader.app.core.util.JsonArrayStrings
import com.polytrader.app.domain.model.Category
import com.polytrader.app.domain.model.EventSummary
import com.polytrader.app.domain.model.MarketSummary
import com.squareup.moshi.JsonClass

/*
 * Gamma API DTOs (gamma-api.polymarket.com). Every field is nullable: Gamma
 * omits null fields from responses. `outcomes`, `outcomePrices` and
 * `clobTokenIds` arrive as JSON-encoded strings and get a second decode in
 * the mappers below.
 */

@JsonClass(generateAdapter = true)
data class GammaMarketDto(
    val id: String?,
    val question: String? = null,
    val conditionId: String? = null,
    val slug: String? = null,
    val description: String? = null,
    val category: String? = null,
    val groupItemTitle: String? = null,
    val resolutionSource: String? = null,
    val outcomes: String? = null,
    val outcomePrices: String? = null,
    val clobTokenIds: String? = null,
    val bestBid: Double? = null,
    val bestAsk: Double? = null,
    val spread: Double? = null,
    val lastTradePrice: Double? = null,
    val oneHourPriceChange: Double? = null,
    val oneDayPriceChange: Double? = null,
    val oneWeekPriceChange: Double? = null,
    val volume: String? = null,
    val liquidity: String? = null,
    val volumeNum: Double? = null,
    val liquidityNum: Double? = null,
    val volume24hr: Double? = null,
    val volume1wk: Double? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val createdAt: String? = null,
    val closedTime: String? = null,
    val active: Boolean? = null,
    val closed: Boolean? = null,
    val archived: Boolean? = null,
    val new: Boolean? = null,
    val featured: Boolean? = null,
    val restricted: Boolean? = null,
    val negRisk: Boolean? = null,
    val enableOrderBook: Boolean? = null,
    val umaResolutionStatus: String? = null,
    val image: String? = null,
    val icon: String? = null,
    val events: List<GammaEventLiteDto>? = null,
    val tags: List<GammaTagDto>? = null,
)

/** Parent-event view nested inside a market response. */
@JsonClass(generateAdapter = true)
data class GammaEventLiteDto(
    val id: String?,
    val slug: String? = null,
    val title: String? = null,
    val ticker: String? = null,
    val image: String? = null,
    val icon: String? = null,
    val negRisk: Boolean? = null,
    val closed: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class GammaEventDto(
    val id: String?,
    val slug: String? = null,
    val title: String? = null,
    val ticker: String? = null,
    val description: String? = null,
    val image: String? = null,
    val icon: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val active: Boolean? = null,
    val closed: Boolean? = null,
    val archived: Boolean? = null,
    val featured: Boolean? = null,
    val new: Boolean? = null,
    val negRisk: Boolean? = null,
    // NOTE: numbers on Event (strings on Market).
    val liquidity: Double? = null,
    val volume: Double? = null,
    val volume24hr: Double? = null,
    val openInterest: Double? = null,
    val commentCount: Int? = null,
    val markets: List<GammaMarketDto>? = null,
    val tags: List<GammaTagDto>? = null,
)

@JsonClass(generateAdapter = true)
data class GammaTagDto(
    val id: String?,
    val label: String? = null,
    val slug: String? = null,
    val forceShow: Boolean? = null,
    val forceHide: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class GammaSearchResponseDto(
    val events: List<GammaEventDto>? = null,
    val tags: List<GammaTagDto>? = null,
    val pagination: GammaPaginationDto? = null,
)

@JsonClass(generateAdapter = true)
data class GammaPaginationDto(
    val hasMore: Boolean? = null,
    val totalResults: Int? = null,
)

@JsonClass(generateAdapter = true)
data class GammaProfileDto(
    val proxyWallet: String? = null,
    val name: String? = null,
    val pseudonym: String? = null,
    val bio: String? = null,
    val profileImage: String? = null,
    val xUsername: String? = null,
    val verifiedBadge: Boolean? = null,
    val createdAt: String? = null,
)

/* ---------------------------------------------------------------------------
 * Mappers
 * ------------------------------------------------------------------------- */

fun GammaTagDto.toDomain(): Category? {
    val id = id ?: return null
    val label = label ?: return null
    return Category(id = id, label = label, slug = slug ?: label.lowercase())
}

fun GammaMarketDto.toDomain(parentEvent: GammaEventDto? = null): MarketSummary? {
    val id = id ?: return null
    val question = question ?: return null
    val slug = slug ?: return null
    val eventLite = events?.firstOrNull()
    return MarketSummary(
        id = id,
        question = question,
        slug = slug,
        conditionId = conditionId,
        eventSlug = eventLite?.slug ?: parentEvent?.slug,
        eventTitle = eventLite?.title ?: parentEvent?.title,
        groupItemTitle = groupItemTitle,
        imageUrl = icon ?: image ?: parentEvent?.icon ?: parentEvent?.image,
        description = description,
        resolutionSource = resolutionSource,
        outcomes = JsonArrayStrings.parseStrings(outcomes),
        outcomePrices = JsonArrayStrings.parseDoubles(outcomePrices),
        clobTokenIds = JsonArrayStrings.parseStrings(clobTokenIds),
        bestBid = bestBid,
        bestAsk = bestAsk,
        spread = spread,
        lastTradePrice = lastTradePrice,
        oneHourChange = oneHourPriceChange,
        oneDayChange = oneDayPriceChange,
        oneWeekChange = oneWeekPriceChange,
        volume = volumeNum ?: volume?.toDoubleOrNull(),
        volume24h = volume24hr,
        liquidity = liquidityNum ?: liquidity?.toDoubleOrNull(),
        startDate = Format.parseInstant(startDate),
        endDate = Format.parseInstant(endDate),
        active = active ?: true,
        closed = closed ?: false,
        isNew = new ?: false,
        featured = featured ?: false,
        negRisk = negRisk ?: false,
        umaResolutionStatus = umaResolutionStatus,
        tags = (tags ?: parentEvent?.tags).orEmpty().mapNotNull { it.toDomain() },
    )
}

fun GammaEventDto.toDomain(): EventSummary? {
    val id = id ?: return null
    val slug = slug ?: return null
    val title = title ?: return null
    return EventSummary(
        id = id,
        slug = slug,
        title = title,
        imageUrl = icon ?: image,
        description = description,
        volume = volume,
        volume24h = volume24hr,
        liquidity = liquidity,
        endDate = Format.parseInstant(endDate),
        closed = closed ?: false,
        negRisk = negRisk ?: false,
        commentCount = commentCount,
        markets = markets.orEmpty().mapNotNull { it.toDomain(parentEvent = this) },
        tags = tags.orEmpty().mapNotNull { it.toDomain() },
    )
}
