package com.polytrader.app.data.ai

import com.polytrader.app.domain.model.MarketSummary

/** Prompt builders for the research features. */
object Prompts {

    const val RESEARCH_SYSTEM =
        "You are PolyTrader's research analyst for Polymarket prediction markets. " +
            "Be concise, quantitative and neutral. Always reason about base rates, " +
            "resolution criteria, time remaining and market microstructure. " +
            "You NEVER give financial advice or tell the user to buy or sell; you " +
            "surface evidence and probability reasoning. When you cite live sources, " +
            "mention them inline."

    fun marketContext(market: MarketSummary): String = buildString {
        appendLine("MARKET CONTEXT")
        appendLine("Question: ${market.question}")
        market.eventTitle?.let { appendLine("Event: $it") }
        appendLine("Outcomes: ${market.outcomes.joinToString()} priced at ${market.outcomePrices.joinToString()}")
        market.probability?.let { appendLine("Implied probability (first outcome): ${(it * 100).toInt()}%") }
        market.volume?.let { appendLine("Total volume: $it USDC") }
        market.liquidity?.let { appendLine("Liquidity: $it USDC") }
        market.endDate?.let { appendLine("Resolution date: $it") }
        market.description?.take(900)?.let { appendLine("Resolution criteria: $it") }
    }

    fun assistantIntro(market: MarketSummary): String =
        "You are answering questions about this specific Polymarket market.\n" +
            marketContext(market) +
            "\nGround answers in the resolution criteria above. If asked something " +
            "unrelated to research, politely redirect."

    /**
     * X sentiment request. The model must return STRICT JSON only, matching
     * [SentimentJson.parse]. Used with Grok x_search (native X) or Gemini
     * grounding (web) — the caller labels the data source accordingly.
     */
    fun sentiment(market: MarketSummary, liveSource: String): String = buildString {
        appendLine(
            "Analyze recent $liveSource discussion (last 7 days) about the following " +
                "prediction-market question and score sentiment toward the YES outcome."
        )
        appendLine(marketContext(market))
        appendLine(
            """
            Respond with STRICT JSON only (no prose, no markdown fences), schema:
            {
              "score": <float -1.0..1.0, sentiment toward YES resolving true>,
              "label": "<Bullish|Leaning Bullish|Mixed|Leaning Bearish|Bearish>",
              "summary": "<2-3 sentence synthesis of the discussion>",
              "quotes": [{"text": "<short representative quote>", "author": "<handle or source>", "stance": "<BULLISH|BEARISH|NEUTRAL>"}],
              "trend": [{"label": "<e.g. 7d ago>", "score": <float -1..1>}, {"label": "3d ago", "score": ...}, {"label": "today", "score": ...}]
            }
            Include 3-5 quotes and exactly 3 trend points (7d ago, 3d ago, today).
            If there is almost no discussion, use score 0, label "Mixed" and say so in summary.
            """.trimIndent()
        )
    }

    /** Probability reasoning: argument map + fair-value estimate as strict JSON. */
    fun probabilityReasoning(market: MarketSummary): String = buildString {
        appendLine("Assess the probability that the FIRST outcome (\"${market.outcomes.firstOrNull() ?: "Yes"}\") resolves true.")
        appendLine(marketContext(market))
        appendLine(
            """
            Respond with STRICT JSON only, schema:
            {
              "fairProbability": <float 0..1, your estimate>,
              "confidence": "<LOW|MEDIUM|HIGH>",
              "bullCase": ["<argument for>", ...],
              "bearCase": ["<argument against>", ...],
              "keyDates": ["<upcoming catalysts with dates>", ...],
              "verdict": "<one sentence comparing your estimate to the market price>"
            }
            Give 2-4 items per list.
            """.trimIndent()
        )
    }
}
