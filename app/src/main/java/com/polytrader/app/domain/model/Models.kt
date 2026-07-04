package com.polytrader.app.domain.model

import java.time.Instant

/* ---------------------------------------------------------------------------
 * Markets & events (mapped from the Gamma API)
 * ------------------------------------------------------------------------- */

data class Category(
    val id: String,
    val label: String,
    val slug: String,
)

/** One tradeable market (binary or one leg of a multi-outcome event). */
data class MarketSummary(
    val id: String,
    val question: String,
    val slug: String,
    val conditionId: String?,
    val eventSlug: String?,
    val eventTitle: String?,
    val groupItemTitle: String?,
    val imageUrl: String?,
    val description: String?,
    val resolutionSource: String?,
    val outcomes: List<String>,
    val outcomePrices: List<Double>,
    val clobTokenIds: List<String>,
    val bestBid: Double?,
    val bestAsk: Double?,
    val spread: Double?,
    val lastTradePrice: Double?,
    val oneHourChange: Double?,
    val oneDayChange: Double?,
    val oneWeekChange: Double?,
    val volume: Double?,
    val volume24h: Double?,
    val liquidity: Double?,
    val startDate: Instant?,
    val endDate: Instant?,
    val active: Boolean,
    val closed: Boolean,
    val isNew: Boolean,
    val featured: Boolean,
    val negRisk: Boolean,
    val umaResolutionStatus: String?,
    val tags: List<Category> = emptyList(),
) {
    /** Probability of the primary ("Yes"/first) outcome, 0..1. */
    val probability: Double? get() = outcomePrices.firstOrNull()

    /** Token id of the primary outcome (used for books/history/WS). */
    val primaryTokenId: String? get() = clobTokenIds.firstOrNull()

    val isBinary: Boolean get() = outcomes.size == 2

    /** For a resolved market, the winning outcome (price ≈ 1). */
    val resolvedOutcome: String? get() =
        if (!closed) null
        else outcomePrices.indexOfFirst { it > 0.99 }.takeIf { it >= 0 }?.let { outcomes.getOrNull(it) }

    /** Display title: prefer the short group item title in multi-outcome events. */
    val displayTitle: String get() = groupItemTitle?.takeIf { it.isNotBlank() } ?: question
}

/** An event groups one or more markets (e.g. "World Cup Winner"). */
data class EventSummary(
    val id: String,
    val slug: String,
    val title: String,
    val imageUrl: String?,
    val description: String?,
    val volume: Double?,
    val volume24h: Double?,
    val liquidity: Double?,
    val endDate: Instant?,
    val closed: Boolean,
    val negRisk: Boolean,
    val commentCount: Int?,
    val markets: List<MarketSummary>,
    val tags: List<Category>,
)

/* ---------------------------------------------------------------------------
 * Live market microstructure (CLOB read-only + WebSocket)
 * ------------------------------------------------------------------------- */

data class BookLevel(val price: Double, val size: Double)

/** Order book with levels normalized best-first. */
data class OrderBook(
    val tokenId: String,
    val bids: List<BookLevel>,
    val asks: List<BookLevel>,
    val timestampMs: Long?,
) {
    val bestBid: Double? get() = bids.firstOrNull()?.price
    val bestAsk: Double? get() = asks.firstOrNull()?.price
    val midpoint: Double? get() {
        val b = bestBid ?: return null
        val a = bestAsk ?: return null
        return (a + b) / 2
    }
    val spread: Double? get() {
        val b = bestBid ?: return null
        val a = bestAsk ?: return null
        return a - b
    }
    val bidDepthUsd: Double get() = bids.sumOf { it.price * it.size }
    val askDepthUsd: Double get() = asks.sumOf { (1 - it.price) * it.size }
}

data class PricePoint(val timeSec: Long, val price: Double)

enum class HistoryRange(val apiInterval: String, val label: String, val fidelityMinutes: Int) {
    H1("1h", "1H", 1),
    H6("6h", "6H", 5),
    D1("1d", "1D", 10),
    W1("1w", "1W", 60),
    M1("1m", "1M", 180),
    ALL("max", "ALL", 720),
}

/** Live updates from the CLOB market WebSocket channel. */
sealed interface MarketWsEvent {
    val tokenId: String

    data class BookSnapshot(
        override val tokenId: String,
        val bids: List<BookLevel>,
        val asks: List<BookLevel>,
        val timestampMs: Long?,
    ) : MarketWsEvent

    data class PriceChange(
        override val tokenId: String,
        val price: Double,
        val size: Double,
        val side: String,
        val bestBid: Double?,
        val bestAsk: Double?,
        val timestampMs: Long?,
    ) : MarketWsEvent

    data class LastTrade(
        override val tokenId: String,
        val price: Double,
        val size: Double,
        val side: String,
        val timestampMs: Long?,
    ) : MarketWsEvent
}

/* ---------------------------------------------------------------------------
 * Portfolio (Data API, read-only by public proxy-wallet address)
 * ------------------------------------------------------------------------- */

data class Position(
    val proxyWallet: String,
    val asset: String,
    val conditionId: String,
    val title: String,
    val slug: String,
    val eventSlug: String?,
    val icon: String?,
    val outcome: String,
    val outcomeIndex: Int,
    val size: Double,
    val avgPrice: Double,
    val curPrice: Double,
    val initialValue: Double,
    val currentValue: Double,
    val cashPnl: Double,
    val percentPnl: Double,
    val realizedPnl: Double,
    val redeemable: Boolean,
    val endDate: Instant?,
    val negativeRisk: Boolean,
)

data class ActivityItem(
    val timestamp: Instant,
    val type: String,
    val side: String?,
    val title: String,
    val slug: String?,
    val eventSlug: String?,
    val icon: String?,
    val outcome: String?,
    val size: Double?,
    val usdcSize: Double?,
    val price: Double?,
    val transactionHash: String?,
)

data class PortfolioSnapshot(
    val wallet: String,
    val totalValue: Double,
    val positions: List<Position>,
) {
    val totalUnrealizedPnl: Double get() = positions.sumOf { it.cashPnl }
    val totalInitialValue: Double get() = positions.sumOf { it.initialValue }
    val redeemableValue: Double get() = positions.filter { it.redeemable }.sumOf { it.currentValue }
}

data class TraderProfile(
    val proxyWallet: String,
    val name: String?,
    val pseudonym: String?,
    val bio: String?,
    val profileImage: String?,
    val xUsername: String?,
    val verifiedBadge: Boolean,
    val createdAt: Instant?,
)

data class MarketTrade(
    val timestamp: Instant,
    val side: String, // BUY | SELL
    val price: Double,
    val size: Double,
    val outcome: String,
    val outcomeIndex: Int,
    val traderName: String,
    val proxyWallet: String?,
    val transactionHash: String?,
) {
    val notionalUsd: Double get() = price * size
}

data class TopHolder(
    val proxyWallet: String,
    val displayName: String,
    val amount: Double,
    val outcomeIndex: Int,
    val profileImage: String?,
)

/* ---------------------------------------------------------------------------
 * Research: sentiment, base rates, correlation, AI
 * ------------------------------------------------------------------------- */

enum class SentimentStance { BULLISH, BEARISH, NEUTRAL }

data class SentimentQuote(
    val text: String,
    val author: String?,
    val stance: SentimentStance,
)

data class SentimentTrendPoint(val label: String, val score: Double)

/** X (Twitter) sentiment for a market, produced by an AI provider with live search. */
data class SentimentReport(
    /** -1 (strongly against the YES outcome) .. +1 (strongly for). */
    val score: Double,
    val label: String,
    val summary: String,
    val quotes: List<SentimentQuote>,
    val trend: List<SentimentTrendPoint>,
    val sources: List<String>,
    val provider: String,
    val hasLiveData: Boolean,
    val generatedAt: Instant,
)

data class CalibrationBucket(
    val label: String,
    val lowerBound: Double,
    val upperBound: Double,
    val sampleCount: Int,
    val favoriteWinRate: Double?,
)

data class ResolvedComparable(
    val marketId: String,
    val question: String,
    val slug: String,
    val eventSlug: String?,
    val endDate: Instant?,
    val priceBeforeClose: Double?,
    val winningOutcome: String?,
    val favoriteWon: Boolean?,
)

/** Base rates computed from similar resolved markets in the same category. */
data class WinRateReport(
    val categoryLabel: String,
    val sampleSize: Int,
    val buckets: List<CalibrationBucket>,
    val comparables: List<ResolvedComparable>,
)

data class RelatedMarket(
    val market: MarketSummary,
    /** Pearson correlation of recent price series with the anchor market, when computed. */
    val correlation: Double?,
)

/* --- AI assistant ---------------------------------------------------------- */

enum class AiProvider(val displayName: String) {
    GEMINI("Gemini"),
    GROK("Grok (xAI)"),
    OPENAI("OpenAI"),
}

data class AiMessage(
    val role: String, // "user" | "assistant"
    val content: String,
)

data class AiAnswer(
    val text: String,
    val citations: List<String> = emptyList(),
    val provider: AiProvider,
)
