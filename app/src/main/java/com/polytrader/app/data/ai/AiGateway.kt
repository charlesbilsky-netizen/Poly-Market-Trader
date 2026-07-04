package com.polytrader.app.data.ai

import com.polytrader.app.domain.model.AiAnswer
import com.polytrader.app.domain.model.AiMessage
import com.polytrader.app.domain.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin, dependency-light clients for the three BYO-key AI providers.
 * Implemented over OkHttp + org.json (rather than typed DTOs) because the
 * response envelopes differ per provider and evolve quickly; each call
 * extracts plain text + citations defensively.
 *
 * Capabilities (July 2026):
 *  - Grok (xAI): `POST /v1/responses` with the server-side `x_search` tool —
 *    the only provider with native X (Twitter) access. Preferred for sentiment.
 *  - Gemini: `generateContent` with `google_search` grounding — free-tier
 *    friendly web sentiment fallback + research assistant.
 *  - OpenAI: plain chat completions — reasoning only, no live data.
 */
class AiGateway(
    baseClient: OkHttpClient,
    /** Returns the current key for a provider, or null/blank when absent. */
    private val keyProvider: (AiProvider) -> String?,
) {
    private val client: OkHttpClient = baseClient.newBuilder()
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun hasKey(provider: AiProvider): Boolean = !keyProvider(provider).isNullOrBlank()

    fun availableProviders(): List<AiProvider> = AiProvider.entries.filter { hasKey(it) }

    /** Provider used for X/web sentiment, in preference order. */
    fun sentimentProvider(): AiProvider? = when {
        hasKey(AiProvider.GROK) -> AiProvider.GROK
        hasKey(AiProvider.GEMINI) -> AiProvider.GEMINI
        hasKey(AiProvider.OPENAI) -> AiProvider.OPENAI
        else -> null
    }

    /**
     * Free-form research chat. [useLiveSearch] lets Grok/Gemini pull in live
     * web/X context; ignored for OpenAI.
     */
    suspend fun chat(
        provider: AiProvider,
        system: String,
        messages: List<AiMessage>,
        useLiveSearch: Boolean = false,
    ): Result<AiAnswer> = withContext(Dispatchers.IO) {
        runCatching {
            when (provider) {
                AiProvider.GROK -> grokResponses(system, messages, useLiveSearch)
                AiProvider.GEMINI -> geminiGenerate(system, messages, useLiveSearch)
                AiProvider.OPENAI -> openAiChat(system, messages)
            }
        }
    }

    /* ----------------------------------------------------------------------
     * Grok — POST https://api.x.ai/v1/responses (agentic tools incl. x_search)
     * -------------------------------------------------------------------- */

    private fun grokResponses(
        system: String,
        messages: List<AiMessage>,
        useLiveSearch: Boolean,
    ): AiAnswer {
        val key = requireNotNull(keyProvider(AiProvider.GROK)) { "No xAI key configured" }
        val input = JSONArray()
        if (system.isNotBlank()) {
            input.put(JSONObject().put("role", "system").put("content", system))
        }
        messages.forEach { input.put(JSONObject().put("role", it.role).put("content", it.content)) }

        val body = JSONObject().apply {
            put("model", "grok-4.3")
            put("input", input)
            if (useLiveSearch) {
                put("tools", JSONArray().put(JSONObject().put("type", "x_search")))
            }
            put("max_output_tokens", 2048)
        }

        val json = execute(
            Request.Builder()
                .url("https://api.x.ai/v1/responses")
                .header("Authorization", "Bearer $key")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
        )

        // output[] items of type "message" -> content[] blocks of type "output_text"
        val text = buildString {
            val output = json.optJSONArray("output") ?: JSONArray()
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val block = content.optJSONObject(j) ?: continue
                    if (block.optString("type") == "output_text") append(block.optString("text"))
                }
            }
        }.ifBlank { json.optString("output_text") } // convenience field, if present

        val citations = json.optJSONArray("citations")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }.orEmpty()

        return AiAnswer(text = text.trim(), citations = citations, provider = AiProvider.GROK)
    }

    /* ----------------------------------------------------------------------
     * Gemini — generateContent (+ google_search grounding)
     * -------------------------------------------------------------------- */

    private fun geminiGenerate(
        system: String,
        messages: List<AiMessage>,
        useGrounding: Boolean,
    ): AiAnswer {
        val key = requireNotNull(keyProvider(AiProvider.GEMINI)) { "No Gemini key configured" }
        val contents = JSONArray()
        messages.forEach { msg ->
            contents.put(
                JSONObject()
                    .put("role", if (msg.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
            )
        }
        val body = JSONObject().apply {
            if (system.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system)))
                )
            }
            put("contents", contents)
            if (useGrounding) {
                put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            }
        }

        val json = execute(
            Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .header("x-goog-api-key", key)
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
        )

        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val text = buildString {
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
            for (i in 0 until parts.length()) {
                parts.optJSONObject(i)?.optString("text")?.let(::append)
            }
        }
        val citations = mutableListOf<String>()
        candidate?.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")?.let { chunks ->
            for (i in 0 until chunks.length()) {
                chunks.optJSONObject(i)?.optJSONObject("web")?.optString("uri")
                    ?.takeIf { it.isNotBlank() }?.let(citations::add)
            }
        }
        return AiAnswer(text = text.trim(), citations = citations, provider = AiProvider.GEMINI)
    }

    /* ----------------------------------------------------------------------
     * OpenAI — chat completions (no live data)
     * -------------------------------------------------------------------- */

    private fun openAiChat(system: String, messages: List<AiMessage>): AiAnswer {
        val key = requireNotNull(keyProvider(AiProvider.OPENAI)) { "No OpenAI key configured" }
        val msgs = JSONArray()
        if (system.isNotBlank()) msgs.put(JSONObject().put("role", "system").put("content", system))
        messages.forEach { msgs.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject()
            .put("model", "gpt-4o-mini")
            .put("messages", msgs)

        val json = execute(
            Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $key")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
        )
        val text = json.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content").orEmpty()
        return AiAnswer(text = text.trim(), citations = emptyList(), provider = AiProvider.OPENAI)
    }

    /* -------------------------------------------------------------------- */

    private fun execute(request: Request): JSONObject {
        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    val o = JSONObject(bodyText)
                    o.optJSONObject("error")?.optString("message")
                        ?.takeIf { it.isNotBlank() } ?: o.optString("error")
                }.getOrNull()?.takeIf { it.isNotBlank() }
                throw AiHttpException(response.code, detail ?: "AI provider returned HTTP ${response.code}")
            }
            return JSONObject(bodyText)
        }
    }
}

class AiHttpException(val code: Int, message: String) : Exception(message)
