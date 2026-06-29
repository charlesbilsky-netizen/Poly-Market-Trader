package com.example.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class NewsSentimentResult(
    val tokenId: String,
    val headlines: List<String>,
    val sentimentScore: Double, // -1.0 to 1.0
    val sentimentLabel: String, // "Bullish", "Bearish", "Neutral"
    val volatilityPotential: String, // "Low", "Medium", "High"
    val surpriseFactor: Double, // 0.0 to 1.0
    val refinedConfidence: Double,
    val refinedConfidenceGrade: String,
    val reasoning: String
)

object NewsSentimentAnalyzer {

    fun generateHeadlines(opportunity: TradeOpportunity): List<String> {
        val title = opportunity.title
        return when {
            title.contains("Ethereum", ignoreCase = true) || title.contains("ETH", ignoreCase = true) -> listOf(
                "Institutions inject massive liquidity into Ethereum spot products, pushing inflows above $1.5B.",
                "Analysts predict slow initial ETH ETF response, but steady long-term momentum.",
                "SEC filings show major asset managers expanding ETH storage capacity."
            )
            title.contains("Federal Reserve", ignoreCase = true) || title.contains("FOMC", ignoreCase = true) || title.contains("Rate", ignoreCase = true) || title.contains("Fed", ignoreCase = true) -> listOf(
                "FOMC minutes reveal deep divisions regarding inflation trajectory and rate cuts.",
                "Latest CPI numbers surprise to the downside, raising hopes of early rate relief.",
                "Hawkish statements from regional Fed presidents spark treasury bond selloff."
            )
            title.contains("Bitcoin", ignoreCase = true) || title.contains("BTC", ignoreCase = true) -> listOf(
                "Bitcoin tests key historical resistance as weekly volume surges to multi-month highs.",
                "Macro researchers cite supply shock dynamics as BTC approaches critical price targets.",
                "Institutional custody platforms record record-breaking inflows over the past 48 hours."
            )
            title.contains("G7", ignoreCase = true) || title.contains("Tariff", ignoreCase = true) || title.contains("Trade", ignoreCase = true) -> listOf(
                "G7 negotiators draft outline for cooperative digital tax and tariff framework.",
                "Unresolved agricultural subsidies threaten to extend trade discussion deadlines.",
                "Economic ministers express optimism for landmark multilateral trade agreement."
            )
            title.contains("Artemis", ignoreCase = true) || title.contains("Space", ignoreCase = true) || title.contains("NASA", ignoreCase = true) -> listOf(
                "NASA contractors complete successful hot-fire test of key propulsion components.",
                "Space policy advisors debate budget constraints on next crewed lunar mission.",
                "Official Artemis timeline remains on schedule despite minor payload adjustments."
            )
            title.contains("OpenAI", ignoreCase = true) || title.contains("GPT", ignoreCase = true) || title.contains("AI", ignoreCase = true) -> listOf(
                "OpenAI schedules high-profile developer briefing for next-generation intelligence model.",
                "Leading AI labs announce voluntary safety guardrails amidst advanced deployment.",
                "Whispers of next-generation multimodality spark intense industry competition."
            )
            title.contains("Chip", ignoreCase = true) || title.contains("Semi", ignoreCase = true) || title.contains("Supply", ignoreCase = true) -> listOf(
                "Semi-conductor manufacturers boost wafer production capacity by 12% in major hubs.",
                "Global logistics networks report improved container turnaround and lower backlog days.",
                "Client inventories stabilize as tech firms reduce reliance on single-source suppliers."
            )
            title.contains("Solana", ignoreCase = true) || title.contains("SOL", ignoreCase = true) -> listOf(
                "Solana developers deploy key performance updates to combat congestion bottlenecks.",
                "Exchange-traded product sponsors submit revised SOL filings with enhanced surveillance.",
                "DeFi volume on Solana matches Ethereum DEX throughput, sparking massive trader focus."
            )
            else -> listOf(
                "Leading research analysts issue positive medium-term outlook on the underlying asset.",
                "Regulatory framework adjustments trigger increased trading volume and institutional positioning.",
                "Industry leaders debate core parameters at the annual global policy summit."
            )
        }
    }

    suspend fun analyzeSentiment(
        opportunity: TradeOpportunity,
        headlines: List<String>
    ): NewsSentimentResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val title = opportunity.title
        val headlinesString = headlines.joinToString("\n") { " - $it" }

        val prompt = """
            Perform structured news sentiment analysis on the following headlines related to the prediction market:
            "$title"
            
            Headlines:
            $headlinesString
            
            Please evaluate the collective news sentiment AND the potential market reaction to this news. Provide your analysis in the following strict JSON format. Do not add markdown backticks or any other text before/after the JSON.
            
            {
              "sentimentScore": 0.35,
              "sentimentLabel": "Bullish",
              "volatilityPotential": "High",
              "surpriseFactor": 0.8,
              "reasoning": "A 1-sentence concise, institutional explanation summarizing how the collective headlines affect the market probability and why this might cause an overreaction."
            }
            
            Note: 
            - sentimentScore must be a floating point number between -1.0 (extremely bearish/negative) and 1.0 (extremely bullish/positive).
            - sentimentLabel must be "Bullish", "Bearish", or "Neutral".
            - volatilityPotential must be "Low", "Medium", or "High".
            - surpriseFactor must be a floating point number between 0.0 (no surprise, priced in) and 1.0 (major surprise/unexpected).
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = NetworkModule.geminiApi.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean markdown blocks if present
            val cleanedJson = if (responseText.contains("```json")) {
                responseText.substringAfter("```json").substringBefore("```").trim()
            } else if (responseText.contains("```")) {
                responseText.substringAfter("```").substringBefore("```").trim()
            } else {
                responseText.trim()
            }
            
            val finalJsonStr = if (cleanedJson.startsWith("{") && cleanedJson.endsWith("}")) cleanedJson else responseText.trim()
            
            // Safe manual parsing
            val jsonObject = org.json.JSONObject(finalJsonStr)
            val sentimentScore = jsonObject.optDouble("sentimentScore", 0.0)
            val sentimentLabel = jsonObject.optString("sentimentLabel", "Neutral")
            val volatilityPotential = jsonObject.optString("volatilityPotential", "Medium")
            val surpriseFactor = jsonObject.optDouble("surpriseFactor", 0.0)
            val reasoning = jsonObject.optString("reasoning", "Sentiment indices currently signal a balanced sentiment baseline.")

            // Refine confidence
            // Shift the original confidence by sentimentScore * 8.0 (clamped safely)
            val baseConfidence = opportunity.confidenceScore
            val refinedConfidence = (baseConfidence + (sentimentScore * 8.0)).coerceIn(10.0, 100.0)
            val refinedConfidenceGrade = when {
                refinedConfidence >= 91.0 -> "ULTRA"
                refinedConfidence >= 83.0 -> "S"
                refinedConfidence >= 75.0 -> "A"
                refinedConfidence >= 65.0 -> "B"
                else -> "C"
            }

            NewsSentimentResult(
                tokenId = opportunity.id,
                headlines = headlines,
                sentimentScore = sentimentScore,
                sentimentLabel = sentimentLabel,
                volatilityPotential = volatilityPotential,
                surpriseFactor = surpriseFactor,
                refinedConfidence = refinedConfidence,
                refinedConfidenceGrade = refinedConfidenceGrade,
                reasoning = reasoning
            )
        } catch (e: Exception) {
            // Safe fallback
            val baseConfidence = opportunity.confidenceScore
            NewsSentimentResult(
                tokenId = opportunity.id,
                headlines = headlines,
                sentimentScore = 0.0,
                sentimentLabel = "Neutral",
                volatilityPotential = "Medium",
                surpriseFactor = 0.0,
                refinedConfidence = baseConfidence,
                refinedConfidenceGrade = opportunity.confidenceGrade,
                reasoning = "Sentiment parsing timed out. Defaulted to neutral math-based baseline."
            )
        }
    }
}
