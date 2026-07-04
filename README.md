# PolyTrader — Polymarket Research Companion

A research-first Android companion app for [Polymarket](https://polymarket.com).
**No trading happens in the app** — every "Open in Polymarket & Trade" button
deep-links to the exact market page on polymarket.com. The app's value is
discovery, live analytics, and AI-powered research.

## Features

- **Discover** — trending / new / ending-soon / resolved rails, category chips,
  debounced full-text search, watchlist stars.
- **Market detail** — live probability via the CLOB WebSocket, animated
  probability bar, historical probability chart (1H → ALL ranges), outcome
  comparison with American odds, order-book liquidity depth view, recent
  trades feed, related markets.
- **AI research** (bring your own key, all optional):
  - **X (Twitter) sentiment** via xAI Grok's native `x_search` tool — score,
    label, key quotes, source citations. Falls back to Gemini with Google
    Search grounding ("web sentiment") or OpenAI (labeled no-live-data).
  - **Probability assessment** — AI fair-value estimate, bull/bear cases,
    verdict vs market price.
- **Scenario calculator** — edge, expected value, win/lose payoff and Kelly
  fraction for a hypothetical stake (pure research math, no orders).
- **Portfolio (read-only)** — paste any public proxy-wallet address to see
  positions, P&L and redemption status via Polymarket's public Data API.
  No keys, no signing, no custody.

All Polymarket data comes from the public, keyless Gamma / CLOB-read / Data
APIs. See `docs/ARCHITECTURE.md` and `docs/API_REFERENCE.md`.

## Build

Prerequisites: Android SDK (platform 36), JDK 17+, Gradle 8.13+ (AGP 8.13).

1. Optionally create `.env` with default AI keys (see `.env.example`) — users
   can also paste keys in-app under Settings.
2. `gradle :app:assembleDebug`
3. Install `app/build/outputs/apk/debug/app-debug.apk`.

## Stack

Jetpack Compose + Material 3 (custom "Meridian" dark-first theme, IBM Plex +
Suez One), Kotlin coroutines/Flow, Retrofit + Moshi, OkHttp WebSocket, Room
(watchlist), DataStore (settings), Coil. Manual DI — no Hilt.
