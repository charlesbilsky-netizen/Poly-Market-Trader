package com.polytrader.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Canonical polymarket.com deep links (verified July 2026).
 *
 * All trading intent in this app funnels through these links — the app itself
 * never places orders. `/market/{slug}` 307-redirects server-side to the
 * canonical `/event/{event-slug}/{market-slug}` page, so it is the most robust
 * target when only a market slug is known.
 */
object PolymarketLinks {

    const val BASE = "https://polymarket.com"

    fun market(marketSlug: String, eventSlug: String? = null): String =
        if (!eventSlug.isNullOrBlank()) "$BASE/event/$eventSlug/$marketSlug"
        else "$BASE/market/$marketSlug"

    fun event(eventSlug: String): String = "$BASE/event/$eventSlug"

    fun profile(proxyWallet: String): String = "$BASE/profile/$proxyWallet"

    fun search(query: String): String = "$BASE/predictions?q=${Uri.encode(query)}"

    fun leaderboard(): String = "$BASE/leaderboard"

    /**
     * Opens a polymarket.com URL. polymarket.com publishes no Android App Links
     * (`assetlinks.json` is absent), so this resolves to the user's browser or
     * an app the user has chosen for the domain.
     */
    fun open(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
