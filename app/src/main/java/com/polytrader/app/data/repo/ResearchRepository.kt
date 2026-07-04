package com.polytrader.app.data.repo

import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.core.net.getOrNull
import com.polytrader.app.core.net.safeApiCall
import com.polytrader.app.data.ai.AiGateway
import com.polytrader.app.data.ai.AiJson
import com.polytrader.app.data.ai.Prompts
import com.polytrader.app.domain.model.AiMessage
import com.polytrader.app.domain.model.AiProvider
import com.polytrader.app.domain.model.CalibrationBucket
import com.polytrader.app.domain.model.HistoryRange
import com.polytrader.app.domain.model.MarketSummary
import com.polytrader.app.domain.model.RelatedMarket
import com.polytrader.app.domain.model.ResolvedComparable
import com.polytrader.app.domain.model.SentimentReport
import com.polytrader.app.domain.model.WinRateReport
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.math.sqrt

data class ProbabilityAssessment(
    val fairProbability: Double,
    val confidence: String,
    val bullCase: List<String>,
    val bearCase: List<String>,
    val keyDates: List<String>,
    val verdict: String,
    val provider: AiProvider,
    val citations: List<String>,
)

/**
 * Research analytics: AI-driven sentiment & reasoning, plus purely
 * quantitative base rates and cross-market correlation computed from public
 * market data. AI results are cached per market for the session.
 */
class ResearchRepository(
    private val markets: MarketRepository,
    private val ai: AiGateway,
) {
    private val sentimentCache = mutableMapOf<String, SentimentReport>()
    private val cacheMutex = Mutex()

    /* ----------------------------------------------------------------------
     * X / web sentiment
     * -------------------------------------------------------------------- */

    suspend fun getSentiment(market: MarketSummary, forceRefresh: Boolean = false): ApiResult<SentimentReport> {
        cacheMutex.withLock {
            if (!forceRefresh) sentimentCache[market.id]?.let { return ApiResult.Success(it) }
        }
        val provider = ai.sentimentProvider()
            ?: return ApiResult.Error("Add an AI key in Settings to unlock sentiment analysis")

        val liveSource = when (provider) {
            AiProvider.GROK -> "X (Twitter)"
            AiProvider.GEMINI -> "web and news"
            AiProvider.OPENAI -> "publicly known"
        }
        val useLive = provider != AiProvider.OPENAI

        val answer = ai.chat(
            provider = provider,
            system = Prompts.RESEARCH_SYSTEM,
            messages = listOf(AiMessage("user", Prompts.sentiment(market, liveSource))),
            useLiveSearch = useLive,
        ).getOrElse { return ApiResult.Error(it.message ?: "Sentiment request failed", it) }

        val parsed = AiJson.parseSentiment(answer.text)
            ?: return ApiResult.Error("Could not parse sentiment response — try refreshing")

        val report = SentimentReport(
            score = parsed.score,
            label = parsed.label,
            summary = parsed.summary,
            quotes = parsed.quotes,
            trend = parsed.trend,
            sources = answer.citations,
            provider = provider.displayName,
            hasLiveData = useLive,
            generatedAt = Instant.now(),
        )
        cacheMutex.withLock { sentimentCache[market.id] = report }
        return ApiResult.Success(report)
    }

    /* ----------------------------------------------------------------------
     * AI probability reasoning
     * -------------------------------------------------------------------- */

    suspend fun assessProbability(market: MarketSummary): ApiResult<ProbabilityAssessment> {
        val provider = ai.sentimentProvider()
            ?: return ApiResult.Error("Add an AI key in Settings to unlock AI analysis")
        val answer = ai.chat(
            provider = provider,
            system = Prompts.RESEARCH_SYSTEM,
            messages = listOf(AiMessage("user", Prompts.probabilityReasoning(market))),
            useLiveSearch = provider != AiProvider.OPENAI,
        ).getOrElse { return ApiResult.Error(it.message ?: "AI analysis failed", it) }

        val parsed = AiJson.parseReasoning(answer.text)
            ?: return ApiResult.Error("Could not parse AI analysis — try again")
        return ApiResult.Success(
            ProbabilityAssessment(
                fairProbability = parsed.fairProbability,
                confidence = parsed.confidence,
                bullCase = parsed.bullCase,
                bearCase = parsed.bearCase,
                keyDates = parsed.keyDates,
                verdict = parsed.verdict,
                provider = answer.provider,
                citations = answer.citations,
            )
        )
    }

    /* ----------------------------------------------------------------------
     * Research assistant chat
     * -------------------------------------------------------------------- */

    suspend fun askAssistant(
        market: MarketSummary,
        history: List<AiMessage>,
        provider: AiProvider,
        useLiveSearch: Boolean,
    ) = ai.chat(
        provider = provider,
        system = Prompts.RESEARCH_SYSTEM + "\n\n" + Prompts.assistantIntro(market),
        messages = history,
        useLiveSearch = useLiveSearch && provider != AiProvider.OPENAI,
    )

    /* ----------------------------------------------------------------------
     * Historical base rates (pure data, no AI)
     * -------------------------------------------------------------------- */

    /**
     * Computes how often the market favorite actually won among resolved
     * markets in the same category: fetches comparables' price ~7 days before
     * close and buckets them by favorite price.
     */
    suspend fun getWinRates(market: MarketSummary, sampleLimit: Int = 14): ApiResult<WinRateReport> =
        safeApiCall {
            val tag = market.tags.firstOrNull()
            val resolved = markets.getResolvedByTag(tag?.id?.toIntOrNull(), limit = 60)
                .getOrNull().orEmpty()
                .filter { it.id != market.id && it.isBinary && it.primaryTokenId != null }
                .take(sampleLimit)

            val comparables = coroutineScope {
                resolved.map { m ->
                    async {
                        val endSec = (m.endDate ?: Instant.now()).epochSecond
                        val history = markets.getPriceHistoryWindow(
                            tokenId = m.primaryTokenId!!,
                            startTs = endSec - 9L * 24 * 3600,
                            endTs = endSec - 6L * 24 * 3600,
                            fidelityMinutes = 720,
                        ).getOrNull().orEmpty()
                        val price7d = history.lastOrNull()?.price
                        val winner = m.resolvedOutcome
                        val favoriteIndex = if ((price7d ?: 0.5) >= 0.5) 0 else 1
                        val favoriteWon = if (price7d == null || winner == null) null
                        else m.outcomes.getOrNull(favoriteIndex) == winner
                        ResolvedComparable(
                            marketId = m.id,
                            question = m.question,
                            slug = m.slug,
                            eventSlug = m.eventSlug,
                            endDate = m.endDate,
                            priceBeforeClose = price7d,
                            winningOutcome = winner,
                            favoriteWon = favoriteWon,
                        )
                    }
                }.map { it.await() }
            }.filter { it.priceBeforeClose != null }

            val bucketRanges = listOf(
                0.50 to 0.60, 0.60 to 0.70, 0.70 to 0.80, 0.80 to 0.90, 0.90 to 1.001,
            )
            val buckets = bucketRanges.map { (lo, hi) ->
                val inBucket = comparables.filter {
                    val fav = it.priceBeforeClose!!.let { p -> if (p >= 0.5) p else 1 - p }
                    fav >= lo && fav < hi
                }
                val decided = inBucket.mapNotNull { it.favoriteWon }
                CalibrationBucket(
                    label = "${(lo * 100).toInt()}–${(hi * 100).toInt().coerceAtMost(100)}¢",
                    lowerBound = lo,
                    upperBound = hi,
                    sampleCount = decided.size,
                    favoriteWinRate = if (decided.isEmpty()) null
                    else decided.count { it }.toDouble() / decided.size,
                )
            }
            WinRateReport(
                categoryLabel = tag?.label ?: "All markets",
                sampleSize = comparables.size,
                buckets = buckets,
                comparables = comparables.sortedByDescending { it.endDate },
            )
        }

    /* ----------------------------------------------------------------------
     * Related markets + correlation
     * -------------------------------------------------------------------- */

    suspend fun getRelatedWithCorrelation(market: MarketSummary): ApiResult<List<RelatedMarket>> =
        safeApiCall {
            val related = coroutineScope {
                val siblings = async { markets.getSiblingMarkets(market).getOrNull().orEmpty() }
                val byTag = async { markets.getRelatedByTag(market).getOrNull().orEmpty() }
                (siblings.await() + byTag.await()).distinctBy { it.id }.take(10)
            }
            if (related.isEmpty()) return@safeApiCall emptyList()

            val anchorToken = market.primaryTokenId
            val anchorSeries = anchorToken?.let {
                markets.getPriceHistory(it, HistoryRange.W1).getOrNull()
            }.orEmpty()

            coroutineScope {
                related.map { m ->
                    async {
                        val corr = m.primaryTokenId?.let { token ->
                            val series = markets.getPriceHistory(token, HistoryRange.W1)
                                .getOrNull().orEmpty()
                            pearson(anchorSeries.associate { it.timeSec to it.price },
                                series.associate { it.timeSec to it.price })
                        }
                        RelatedMarket(market = m, correlation = corr)
                    }
                }.map { it.await() }
            }.sortedByDescending { it.market.volume24h ?: 0.0 }
        }

    /** Pearson correlation over aligned hourly buckets; null when overlap is thin. */
    private fun pearson(a: Map<Long, Double>, b: Map<Long, Double>): Double? {
        if (a.isEmpty() || b.isEmpty()) return null
        fun bucket(m: Map<Long, Double>) = m.entries.groupBy { it.key / 3600 }
            .mapValues { (_, v) -> v.map { it.value }.average() }
        val ba = bucket(a)
        val bb = bucket(b)
        val keys = ba.keys.intersect(bb.keys).sorted()
        if (keys.size < 8) return null
        val xs = keys.map { ba.getValue(it) }
        val ys = keys.map { bb.getValue(it) }
        val mx = xs.average()
        val my = ys.average()
        var num = 0.0; var dx = 0.0; var dy = 0.0
        for (i in keys.indices) {
            val vx = xs[i] - mx
            val vy = ys[i] - my
            num += vx * vy; dx += vx * vx; dy += vy * vy
        }
        if (dx == 0.0 || dy == 0.0) return null
        return (num / sqrt(dx * dy)).coerceIn(-1.0, 1.0)
    }
}
