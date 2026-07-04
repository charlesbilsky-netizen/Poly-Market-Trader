package com.polytrader.app.data.repo

import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.core.net.safeApiCall
import com.polytrader.app.data.remote.clob.ClobApiService
import com.polytrader.app.data.remote.clob.toDomain
import com.polytrader.app.data.remote.dataapi.DataApiService
import com.polytrader.app.data.remote.dataapi.toDomain
import com.polytrader.app.data.remote.gamma.GammaApiService
import com.polytrader.app.data.remote.gamma.toDomain
import com.polytrader.app.domain.model.Category
import com.polytrader.app.domain.model.EventSummary
import com.polytrader.app.domain.model.HistoryRange
import com.polytrader.app.domain.model.MarketSummary
import com.polytrader.app.domain.model.MarketTrade
import com.polytrader.app.domain.model.OrderBook
import com.polytrader.app.domain.model.PricePoint
import com.polytrader.app.domain.model.TopHolder
import java.time.Instant
import java.time.format.DateTimeFormatter

/** Discovery sections shown on the home screen. */
enum class DiscoverSection { TRENDING, NEW, ENDING_SOON, RESOLVED }

class MarketRepository(
    private val gamma: GammaApiService,
    private val clob: ClobApiService,
    private val dataApi: DataApiService,
) {
    private val iso = DateTimeFormatter.ISO_INSTANT

    /**
     * Loads one discovery rail. Markets are the unit of display; junk is
     * filtered with liquidity/volume floors. `active`/`archived` are response
     * fields only on /markets, so open-market filtering happens client-side.
     */
    suspend fun getSection(
        section: DiscoverSection,
        tagId: Int? = null,
        limit: Int = 40,
        offset: Int = 0,
    ): ApiResult<List<MarketSummary>> = safeApiCall {
        val now = Instant.now()
        val dtos = when (section) {
            DiscoverSection.TRENDING -> gamma.getMarkets(
                limit = limit, offset = offset,
                order = "volume24hr", ascending = false,
                closed = false, tagId = tagId, relatedTags = tagId?.let { true },
                liquidityMin = 1_000.0,
            )
            DiscoverSection.NEW -> gamma.getMarkets(
                limit = limit, offset = offset,
                order = "startDate", ascending = false,
                closed = false, tagId = tagId, relatedTags = tagId?.let { true },
                liquidityMin = 500.0,
            )
            DiscoverSection.ENDING_SOON -> gamma.getMarkets(
                limit = limit, offset = offset,
                order = "endDate", ascending = true,
                closed = false, tagId = tagId, relatedTags = tagId?.let { true },
                endDateMin = iso.format(now),
                endDateMax = iso.format(now.plusSeconds(14L * 24 * 3600)),
                liquidityMin = 500.0,
            )
            DiscoverSection.RESOLVED -> gamma.getMarkets(
                limit = limit, offset = offset,
                order = "endDate", ascending = false,
                closed = true, tagId = tagId, relatedTags = tagId?.let { true },
                volumeMin = 10_000.0,
            )
        }
        val open = section != DiscoverSection.RESOLVED
        dtos.mapNotNull { it.toDomain() }
            .filter { if (open) it.active && !it.closed else it.closed }
            .filter { it.outcomePrices.isNotEmpty() }
    }

    /** Featured/high-volume events for the hero carousel. */
    suspend fun getFeaturedEvents(limit: Int = 10): ApiResult<List<EventSummary>> = safeApiCall {
        gamma.getEvents(
            limit = limit,
            order = "volume24hr", ascending = false,
            closed = false, active = true, featured = true,
        ).mapNotNull { it.toDomain() }
            .ifEmpty {
                gamma.getEvents(
                    limit = limit, order = "volume24hr", ascending = false,
                    closed = false, active = true,
                ).mapNotNull { it.toDomain() }
            }
    }

    /** Main category chips (carousel tags), with a curated fallback. */
    suspend fun getCategories(): ApiResult<List<Category>> = safeApiCall {
        val carousel = runCatching {
            gamma.getTags(limit = 60, isCarousel = true).mapNotNull { it.toDomain() }
        }.getOrDefault(emptyList())
        carousel.ifEmpty {
            gamma.getTags(limit = 60).mapNotNull { it.toDomain() }
        }
    }

    /** Full-text search via Gamma public-search (events + nested markets). */
    suspend fun search(query: String): ApiResult<List<MarketSummary>> = safeApiCall {
        val response = gamma.search(query = query, limitPerType = 20)
        response.events.orEmpty()
            .mapNotNull { it.toDomain() }
            .flatMap { event -> event.markets.filter { !it.closed } }
            .distinctBy { it.id }
    }

    suspend fun getMarket(marketId: String): ApiResult<MarketSummary> = safeApiCall {
        gamma.getMarket(marketId).toDomain() ?: error("Market $marketId not found")
    }

    /** Sibling markets in the same event (e.g. other candidates in a race). */
    suspend fun getSiblingMarkets(market: MarketSummary): ApiResult<List<MarketSummary>> =
        safeApiCall {
            val eventSlug = market.eventSlug ?: return@safeApiCall emptyList()
            gamma.getEventBySlug(eventSlug).toDomain()
                ?.markets
                ?.filter { it.id != market.id }
                .orEmpty()
        }

    /** Markets sharing the anchor's primary tag, by 24h volume. */
    suspend fun getRelatedByTag(market: MarketSummary, limit: Int = 12): ApiResult<List<MarketSummary>> =
        safeApiCall {
            val tagId = market.tags.firstOrNull()?.id?.toIntOrNull()
                ?: return@safeApiCall emptyList()
            gamma.getMarkets(
                limit = limit + 4,
                order = "volume24hr", ascending = false,
                closed = false, tagId = tagId, relatedTags = true,
                liquidityMin = 1_000.0,
            ).mapNotNull { it.toDomain() }
                .filter { it.id != market.id && it.eventSlug != market.eventSlug }
                .take(limit)
        }

    /** Resolved markets in a tag, for base-rate/win-rate analysis. */
    suspend fun getResolvedByTag(tagId: Int?, limit: Int = 60): ApiResult<List<MarketSummary>> =
        safeApiCall {
            gamma.getMarkets(
                limit = limit,
                order = "endDate", ascending = false,
                closed = true, tagId = tagId, relatedTags = tagId?.let { true },
                volumeMin = 5_000.0,
            ).mapNotNull { it.toDomain() }
                .filter { it.resolvedOutcome != null }
        }

    suspend fun getPriceHistory(
        tokenId: String,
        range: HistoryRange,
    ): ApiResult<List<PricePoint>> = safeApiCall {
        clob.getPriceHistory(
            tokenId = tokenId,
            interval = range.apiInterval,
            fidelityMinutes = range.fidelityMinutes,
        ).toDomain()
    }

    suspend fun getPriceHistoryWindow(
        tokenId: String,
        startTs: Long,
        endTs: Long,
        fidelityMinutes: Int,
    ): ApiResult<List<PricePoint>> = safeApiCall {
        clob.getPriceHistory(
            tokenId = tokenId,
            startTs = startTs,
            endTs = endTs,
            fidelityMinutes = fidelityMinutes,
        ).toDomain()
    }

    suspend fun getOrderBook(tokenId: String): ApiResult<OrderBook> = safeApiCall {
        clob.getBook(tokenId).toDomain() ?: error("No order book for token")
    }

    suspend fun getRecentTrades(
        conditionId: String,
        limit: Int = 30,
        minCashUsd: Double? = null,
    ): ApiResult<List<MarketTrade>> = safeApiCall {
        dataApi.getTrades(
            conditionId = conditionId,
            limit = limit,
            filterType = minCashUsd?.let { "CASH" },
            filterAmount = minCashUsd,
        ).mapNotNull { it.toDomain() }
    }

    suspend fun getTopHolders(conditionId: String): ApiResult<Map<String, List<TopHolder>>> =
        safeApiCall {
            dataApi.getHolders(conditionIds = conditionId)
                .filter { it.token != null }
                .associate { meta -> meta.token!! to meta.holders.orEmpty().mapNotNull { it.toDomain() } }
        }
}
