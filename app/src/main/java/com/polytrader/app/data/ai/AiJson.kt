package com.polytrader.app.data.ai

import com.polytrader.app.domain.model.SentimentQuote
import com.polytrader.app.domain.model.SentimentStance
import com.polytrader.app.domain.model.SentimentTrendPoint
import org.json.JSONObject

/**
 * Lenient parsing of "strict JSON" LLM replies: tolerates markdown fences,
 * leading prose and trailing junk by extracting the outermost object.
 */
object AiJson {

    fun extractObject(raw: String): JSONObject? {
        val cleaned = raw
            .replace("```json", "```")
            .substringAfter("```", raw)
            .substringBefore("```")
        val start = cleaned.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until cleaned.length) {
            when (cleaned[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return runCatching { JSONObject(cleaned.substring(start, i + 1)) }.getOrNull()
                    }
                }
            }
        }
        return runCatching { JSONObject(cleaned.substring(start)) }.getOrNull()
    }

    data class ParsedSentiment(
        val score: Double,
        val label: String,
        val summary: String,
        val quotes: List<SentimentQuote>,
        val trend: List<SentimentTrendPoint>,
    )

    fun parseSentiment(raw: String): ParsedSentiment? {
        val json = extractObject(raw) ?: return null
        if (!json.has("score")) return null
        val quotes = json.optJSONArray("quotes")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val q = arr.optJSONObject(i) ?: return@mapNotNull null
                val text = q.optString("text").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SentimentQuote(
                    text = text,
                    author = q.optString("author").takeIf { it.isNotBlank() },
                    stance = when (q.optString("stance").uppercase()) {
                        "BULLISH" -> SentimentStance.BULLISH
                        "BEARISH" -> SentimentStance.BEARISH
                        else -> SentimentStance.NEUTRAL
                    },
                )
            }
        }.orEmpty()
        val trend = json.optJSONArray("trend")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val t = arr.optJSONObject(i) ?: return@mapNotNull null
                SentimentTrendPoint(
                    label = t.optString("label").ifBlank { "t$i" },
                    score = t.optDouble("score", 0.0).coerceIn(-1.0, 1.0),
                )
            }
        }.orEmpty()
        return ParsedSentiment(
            score = json.optDouble("score", 0.0).coerceIn(-1.0, 1.0),
            label = json.optString("label").ifBlank { "Mixed" },
            summary = json.optString("summary"),
            quotes = quotes,
            trend = trend,
        )
    }

    data class ParsedReasoning(
        val fairProbability: Double,
        val confidence: String,
        val bullCase: List<String>,
        val bearCase: List<String>,
        val keyDates: List<String>,
        val verdict: String,
    )

    fun parseReasoning(raw: String): ParsedReasoning? {
        val json = extractObject(raw) ?: return null
        if (!json.has("fairProbability")) return null
        fun list(key: String): List<String> =
            json.optJSONArray(key)?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
            }.orEmpty()
        return ParsedReasoning(
            fairProbability = json.optDouble("fairProbability", 0.5).coerceIn(0.0, 1.0),
            confidence = json.optString("confidence").ifBlank { "LOW" },
            bullCase = list("bullCase"),
            bearCase = list("bearCase"),
            keyDates = list("keyDates"),
            verdict = json.optString("verdict"),
        )
    }
}
