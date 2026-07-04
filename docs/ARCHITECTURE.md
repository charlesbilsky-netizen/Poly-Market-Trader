# PolyTrader — Polymarket Research Companion

PolyTrader is a **research-first companion app for Polymarket**. It never executes
trades. Every "Trade" action deep-links to the corresponding market page on
polymarket.com (which opens the official Polymarket app when installed, or the web
otherwise). The app's value is discovery, analytics, AI research, and a clean
hand-off into Polymarket.

## Product pillars

1. **Market Discovery** — trending / new / ending-soon / resolved rails, category
   filters, full-text search, sorting, watchlist personalization.
2. **Market Detail** — live probability with smooth animation (WebSocket), price
   history chart with volume overlay, outcome comparison, order-book depth view.
3. **Research Suite** — AI research assistant (BYO Gemini / xAI Grok / OpenAI key),
   X (Twitter) sentiment via Grok Live Search or Gemini Search grounding, historical
   base rates from resolved markets, scenario/edge calculator, related-market
   correlation, exportable branded PDF research reports.
4. **Portfolio (read-only)** — positions, P&L, activity and redemption status for
   any wallet address via Polymarket's public Data API. No keys, no signing.
5. **Redirect excellence** — contextual "Open in Polymarket" buttons everywhere a
   trade decision could happen.

## Non-goals (enforced)

- No order placement, signing, or CLOB credentials. The legacy HMAC interceptor,
  order DTOs and `POST /order` code paths were removed.
- No custody, no wallet keys — portfolio is by public address only.

## Stack

- Jetpack Compose + Material 3, single-activity, Navigation Compose.
- Kotlin coroutines + Flow end to end.
- Retrofit + Moshi (codegen) + OkHttp (REST), OkHttp WebSocket (live prices).
- Room (watchlist, recent searches), DataStore Preferences (settings).
- Coil (market imagery), custom Canvas charts (no chart dependency).
- Manual DI via `AppContainer` (no Hilt — kept deliberately simple).

## Data sources (all public, keyless)

| Source | Base URL | Used for |
|---|---|---|
| Gamma API | `https://gamma-api.polymarket.com/` | events, markets, tags, search |
| CLOB API (read-only) | `https://clob.polymarket.com/` | order book, midpoints, price history |
| Data API | `https://data-api.polymarket.com/` | positions, activity, portfolio value, holders, trades |
| Market WS | `wss://ws-subscriptions-clob.polymarket.com/ws/market` | live `book` / `price_change` / `last_trade_price` events |
| AI providers (BYO key) | Gemini / xAI / OpenAI | research assistant, X sentiment, probability reasoning |

Gotchas encoded in the data layer:

- Gamma returns `outcomes`, `outcomePrices`, `clobTokenIds` as **JSON-encoded
  strings** (e.g. `"[\"Yes\", \"No\"]"`), decoded by `JsonArrayStrings`.
- `/prices-history` takes `market=<clobTokenId>` and either `interval`
  (`1h,6h,1d,1w,1m,max`) or `startTs/endTs` + `fidelity` minutes; returns
  `{"history":[{"t":<unix seconds>,"p":<0..1>}]}`.
- The WS market channel expects `{"assets_ids":[...],"type":"market"}` on connect
  and a text `PING` keepalive every ~10s.
- Data API expects the user's **proxy wallet** address (the address shown in a
  polymarket.com profile URL).

## Package layout (`com.polytrader.app`)

```
PolyTraderApp.kt                     Application; owns AppContainer
MainActivity.kt                      single activity; theme + nav host
di/AppContainer.kt                   manual DI graph + ViewModel factories
core/util/                           Format, PolymarketLinks, JsonArrayStrings, TimeAgo
core/net/ApiResult.kt                sealed result + safeApiCall
data/remote/gamma/                   GammaApiService + DTOs
data/remote/clob/                    ClobApiService + DTOs (read-only endpoints only)
data/remote/dataapi/                 DataApiService + DTOs
data/remote/ws/MarketWebSocketClient WS client -> Flow<MarketWsEvent>
data/remote/NetworkModule.kt         Retrofit/OkHttp wiring (no auth)
data/ai/                             AiGateway (provider routing), models, prompts
data/db/                             Room: watchlist + recent searches
data/settings/SettingsRepository.kt  DataStore: wallet addr, AI keys, prefs
data/repo/MarketRepository.kt        discovery, detail, history, book, related
data/repo/PortfolioRepository.kt     positions/activity/value
data/repo/ResearchRepository.kt      sentiment, base rates, correlation, reports
domain/model/Models.kt               UI-facing domain models (mapped from DTOs)
ui/theme/                            Meridian design system (dark-first M3)
ui/components/                       MarketCard, AnimatedProbabilityBar, charts, tiles…
ui/nav/AppNavHost.kt                 routes
ui/discover|detail|research|portfolio|watchlist|settings/
```

## Navigation

Bottom bar: **Discover · Watchlist · Portfolio**. Settings via top-bar gear.
`market/{marketId}` is pushed from anywhere; `marketId` is the Gamma market id.

## Screens → repositories

- `DiscoverViewModel` → `MarketRepository`
  (sections: trending by `volume24hr`, new by `startDate`, ending-soon by
  `endDate` ascending with `end_date_min=now`, recently-resolved `closed=true`;
  category chips from `/tags`; search via Gamma `/public-search`).
- `MarketDetailViewModel` → `MarketRepository` + WS + `ResearchRepository` + `AiGateway`.
- `PortfolioViewModel` → `PortfolioRepository` + `SettingsRepository`.
- `WatchlistViewModel` → Room + `MarketRepository` (refresh quotes).
- `SettingsViewModel` → `SettingsRepository`.

## AI provider routing

`AiGateway` picks the best available provider per capability:

| Capability | 1st choice | 2nd | 3rd |
|---|---|---|---|
| X sentiment | Grok (native X live search) | Gemini (Google Search grounding) | OpenAI (no live data — clearly labeled) |
| Research chat | any configured (user-selectable) | | |
| Probability reasoning | any configured | | |

Responses that must be structured (sentiment score, quotes, trend) are requested
as strict JSON and parsed leniently (fenced-block tolerant).

## Design system ("Meridian")

Dark-first Material 3 with a hand-tuned palette (deep ink `#0B0F1A` background,
indigo `#6C8CFF` primary, aqua `#38E1C6` secondary, YES `#2FD576` / NO `#FF5C7A`,
gold `#F5B84C` for AI accents), IBM Plex Sans / IBM Plex Mono / Suez One via
downloadable Google Fonts (system fallbacks bundled), 16dp rounded surfaces,
hairline `#232D40` borders, subtle vertical gradients. A matching light scheme
ships too; theme follows system with an override in Settings.

## Verification

- `./…/gradle :app:compileDebugKotlin` (or `assembleDebug`) must pass.
- Robolectric + Roborazzi screenshot smoke test for the theme.
- Unit tests: Gamma JSON-string-array decoding, scenario math, deep-link builder.
