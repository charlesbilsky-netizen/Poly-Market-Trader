package com.example.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppPrefs
import com.example.data.ResearchReportEntity
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GrokChatRequest
import com.example.network.GrokMessage
import com.example.network.NetworkModule
import com.example.network.OpenAiChatRequest
import com.example.network.OpenAiMessage
import com.example.network.Part
import com.example.network.parseJsonDoubleArray
import java.util.Locale
import java.util.concurrent.TimeUnit

/*
 * Background market intelligence (Phase 2.3 / 2.4). Read-only research:
 * these workers fetch public data, synthesize AI digests, and post local
 * notifications. Nothing here can trade.
 */

object MarketIntel {
    const val DIGEST_CHANNEL = "quant_digest"
    const val WHALE_CHANNEL = "whale_watch"
    const val EXTRA_REPORT_TITLE = "report_title"
    const val EXTRA_REPORT_CONTENT = "report_content"

    /** Idempotent: schedules the daily digest + 6h whale watch. */
    fun schedule(context: Context) {
        createChannels(context)
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            "daily_quant_digest",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MarketDigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            "whale_watch",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WhaleWatchWorker>(6, TimeUnit.HOURS)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build(),
        )
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                DIGEST_CHANNEL, "Daily Quant Digest", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Once-a-day AI intelligence report on trending markets" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                WHALE_CHANNEL, "Whale Watch", NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Large position moves on watchlisted markets" },
        )
    }

    fun notify(context: Context, channel: String, id: Int, title: String, body: String, reportTitle: String? = null, reportContent: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (reportContent != null) {
                putExtra(EXTRA_REPORT_TITLE, reportTitle ?: title)
                putExtra(EXTRA_REPORT_CONTENT, reportContent)
            }
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(600)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Shared AI routing for background prompts: Gemini -> Grok -> OpenAI. */
    suspend fun runAiPrompt(context: Context, systemHint: String, prompt: String): String? {
        val google = AppPrefs.googleKey(context)
        val xai = AppPrefs.xaiKey(context)
        val openai = AppPrefs.openaiKey(context)
        return try {
            when {
                google.isNotBlank() -> {
                    NetworkModule.geminiApi.generateContent(
                        google,
                        GenerateContentRequest(listOf(Content(listOf(Part(text = "$systemHint\n\n$prompt"))))),
                    ).candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                }
                xai.isNotBlank() -> {
                    NetworkModule.grokApi.getChatCompletions(
                        "Bearer $xai",
                        GrokChatRequest(listOf(GrokMessage("system", systemHint), GrokMessage("user", prompt))),
                    ).choices?.firstOrNull()?.message?.content
                }
                openai.isNotBlank() -> {
                    NetworkModule.openAiApi.getChatCompletions(
                        "Bearer $openai",
                        OpenAiChatRequest(listOf(OpenAiMessage("system", systemHint), OpenAiMessage("user", prompt))),
                    ).choices?.firstOrNull()?.message?.content
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Once a day: pull the top trending markets from Gamma, run the configured AI
 * over them with the user's custom instructions, persist the digest to Room
 * and surface it as a notification that deep-links into the report viewer.
 */
class MarketDigestWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val events = NetworkModule.polymarketApi.getEvents(limit = 8)
                .filter { !it.title.isNullOrBlank() }
                .sortedByDescending { it.volume24hr ?: it.volume ?: 0.0 }
                .take(3)
            if (events.isEmpty()) return Result.retry()

            val table = events.mapIndexed { i, e ->
                val market = e.markets?.firstOrNull()
                val prob = parseJsonDoubleArray(market?.outcomePrices).firstOrNull()
                    ?.let { "${(it * 100).toInt()}%" } ?: "n/a"
                val vol = e.volume?.let { String.format(Locale.US, "$%,.0f", it) } ?: "n/a"
                "${i + 1}. \"${e.title}\" | implied $prob | volume $vol | ends ${e.endDate ?: "n/a"}"
            }.joinToString("\n")

            val custom = AppPrefs.customInstructions(applicationContext)
                .takeIf { it.isNotBlank() }?.let { "\nUser focus: $it" } ?: ""

            val digest = MarketIntel.runAiPrompt(
                applicationContext,
                "You are PolyTrader's Daily Quant Digest engine — an elite, concise macro strategist for prediction markets. Research only; never advise order execution.",
                """
                Produce today's DAILY QUANT DIGEST for these top-volume Polymarket markets:
                $table
                $custom
                Format (dense, monospace-friendly, < 250 words):
                MARKET PULSE — one-line macro read.
                For each market: implied probability vs your base-rate view, the key catalyst, and what would change the picture.
                Close with WATCH TODAY: the single most information-rich thing to monitor.
                """.trimIndent(),
            ) ?: return Result.retry()

            AppDatabase.get(applicationContext).researchReportDao().insert(
                ResearchReportEntity(
                    type = "DIGEST",
                    marketId = "",
                    title = "Daily Quant Digest",
                    content = digest,
                    provider = "auto",
                    createdAt = System.currentTimeMillis(),
                ),
            )
            MarketIntel.notify(
                applicationContext,
                MarketIntel.DIGEST_CHANNEL,
                1001,
                "Daily Quant Digest ready",
                digest,
                reportTitle = "DAILY QUANT DIGEST",
                reportContent = digest,
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

/**
 * Every 6h: scan watchlisted markets for whale-sized fills (>= $10k notional)
 * since the last check and notify with the largest move.
 */
class WhaleWatchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.get(applicationContext)
            val watched = db.watchlistDao().getAllOnce()
                .filter { !it.conditionId.isNullOrBlank() && it.conditionId!!.startsWith("0x") }
                .take(10)
            if (watched.isEmpty()) return Result.success()

            val sinceSec = AppPrefs.lastWhaleCheck(applicationContext)
                .takeIf { it > 0 } ?: (System.currentTimeMillis() / 1000 - 6 * 3600)

            var notified = 0
            for (entity in watched) {
                if (notified >= 3) break
                val whales = try {
                    NetworkModule.polymarketDataApi.getTrades(
                        conditionId = entity.conditionId!!,
                        limit = 10,
                        filterType = "CASH",
                        filterAmount = 10_000.0,
                    )
                } catch (_: Exception) {
                    continue
                }
                val fresh = whales.filter { (it.timestamp ?: 0) > sinceSec }
                val biggest = fresh.maxByOrNull { it.notionalUsd } ?: continue
                MarketIntel.notify(
                    applicationContext,
                    MarketIntel.WHALE_CHANNEL,
                    2000 + notified,
                    "🐋 ${String.format(Locale.US, "$%,.0f", biggest.notionalUsd)} ${biggest.side ?: "MOVE"} — ${entity.title.take(40)}",
                    "${biggest.traderLabel} ${biggest.side?.lowercase() ?: "traded"} " +
                        "${String.format(Locale.US, "%,.0f", biggest.size ?: 0.0)} \"${biggest.outcome ?: ""}\" shares @ " +
                        String.format(Locale.US, "%.1f¢", (biggest.price ?: 0.0) * 100) +
                        " on ${entity.title}",
                )
                notified++
            }
            AppPrefs.setLastWhaleCheck(applicationContext, System.currentTimeMillis() / 1000)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
