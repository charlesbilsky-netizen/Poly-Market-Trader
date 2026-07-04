package com.polytrader.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.polytrader.app.BuildConfig
import com.polytrader.app.domain.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    /** Public proxy-wallet address for the read-only portfolio view. */
    val walletAddress: String = "",
    val geminiKey: String = "",
    val xaiKey: String = "",
    val openaiKey: String = "",
    /** Preferred assistant provider; sentiment routing is capability-based. */
    val preferredProvider: AiProvider = AiProvider.GEMINI,
    val darkTheme: Boolean = true,
) {
    fun keyFor(provider: AiProvider): String = when (provider) {
        AiProvider.GEMINI -> geminiKey
        AiProvider.GROK -> xaiKey
        AiProvider.OPENAI -> openaiKey
    }
}

/**
 * User preferences. AI keys entered in-app override the optional build-time
 * defaults injected from `.env` by the secrets plugin.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val WALLET = stringPreferencesKey("wallet_address")
        val GEMINI = stringPreferencesKey("gemini_key")
        val XAI = stringPreferencesKey("xai_key")
        val OPENAI = stringPreferencesKey("openai_key")
        val PROVIDER = stringPreferencesKey("preferred_provider")
        val DARK = booleanPreferencesKey("dark_theme")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            walletAddress = prefs[Keys.WALLET].orEmpty(),
            geminiKey = prefs[Keys.GEMINI].orDefaultKey(BuildConfig.GEMINI_API_KEY),
            xaiKey = prefs[Keys.XAI].orDefaultKey(BuildConfig.XAI_API_KEY),
            openaiKey = prefs[Keys.OPENAI].orDefaultKey(BuildConfig.OPENAI_API_KEY),
            preferredProvider = prefs[Keys.PROVIDER]?.let { stored ->
                AiProvider.entries.firstOrNull { it.name == stored }
            } ?: AiProvider.GEMINI,
            darkTheme = prefs[Keys.DARK] ?: true,
        )
    }

    /** Latest snapshot for one-shot reads. */
    suspend fun current(): AppSettings = settings.first()

    suspend fun setWalletAddress(address: String) =
        context.dataStore.edit { it[Keys.WALLET] = address.trim() }

    suspend fun setAiKeys(gemini: String, xai: String, openai: String) =
        context.dataStore.edit {
            it[Keys.GEMINI] = gemini.trim()
            it[Keys.XAI] = xai.trim()
            it[Keys.OPENAI] = openai.trim()
        }

    suspend fun setPreferredProvider(provider: AiProvider) =
        context.dataStore.edit { it[Keys.PROVIDER] = provider.name }

    suspend fun setDarkTheme(dark: Boolean) =
        context.dataStore.edit { it[Keys.DARK] = dark }

    private fun String?.orDefaultKey(buildDefault: String): String {
        val stored = this?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        // Placeholder values from .env.example are not real keys.
        return buildDefault.takeUnless { it.isBlank() || it.startsWith("MY_") }.orEmpty()
    }
}
