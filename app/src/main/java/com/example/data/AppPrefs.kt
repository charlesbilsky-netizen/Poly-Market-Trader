package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

/**
 * Lightweight persisted settings. Keys live on-device only; background
 * workers (daily digest, whale watch) read them because ViewModel state does
 * not exist while the app is closed. Also fixes settings being lost across
 * process restarts.
 */
object AppPrefs {
    private const val NAME = "polytrader_prefs"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun saveAiKeys(context: Context, google: String, xai: String, openai: String) {
        prefs(context).edit()
            .putString("google_key", google)
            .putString("xai_key", xai)
            .putString("openai_key", openai)
            .apply()
    }

    fun saveWallet(context: Context, wallet: String) {
        prefs(context).edit().putString("wallet", wallet).apply()
    }

    fun saveCustomInstructions(context: Context, instructions: String) {
        prefs(context).edit().putString("custom_instructions", instructions).apply()
    }

    private fun key(stored: String?, buildDefault: String): String {
        val s = stored?.trim().orEmpty()
        if (s.isNotEmpty()) return s
        return buildDefault.takeUnless { it.isBlank() || it.startsWith("MY_") }.orEmpty()
    }

    fun googleKey(context: Context): String =
        key(prefs(context).getString("google_key", null), BuildConfig.GEMINI_API_KEY)

    fun xaiKey(context: Context): String =
        key(prefs(context).getString("xai_key", null), BuildConfig.XAI_API_KEY)

    fun openaiKey(context: Context): String =
        key(prefs(context).getString("openai_key", null), BuildConfig.OPENAI_API_KEY)

    fun wallet(context: Context): String =
        prefs(context).getString("wallet", "").orEmpty()

    fun customInstructions(context: Context): String =
        prefs(context).getString("custom_instructions", "").orEmpty()

    fun lastWhaleCheck(context: Context): Long =
        prefs(context).getLong("last_whale_check", 0L)

    fun setLastWhaleCheck(context: Context, timestampSec: Long) {
        prefs(context).edit().putLong("last_whale_check", timestampSec).apply()
    }
}
