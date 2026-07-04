# PolyTrader — Polymarket Research Companion

PolyTrader is a **research-first companion app for Polymarket**. It never executes trades. Every "Trade" action deep-links to the corresponding market page on polymarket.com. The app's value is discovery, analytics, AI research, and a clean hand-off into Polymarket.

## Product Pillars
1. **Market Discovery** — trending / new / ending-soon / resolved rails, category filters, full-text search, sorting, watchlist personalization.
2. **Market Detail** — live probability with smooth animation (WebSocket), price history chart with volume overlay, outcome comparison, order-book depth view.
3. **Research Suite** — AI research assistant (BYO Gemini / xAI Grok / OpenAI key), X (Twitter) sentiment via Grok Live Search or Gemini Search grounding, historical base rates from resolved markets, scenario/edge calculator, related-market correlation, exportable branded PDF research reports.
4. **Portfolio (read-only)** — positions, P&L, activity and redemption status for any wallet address via Polymarket's public Data API. No keys, no signing.
5. **Redirect excellence** — contextual "Open in Polymarket" buttons everywhere a trade decision could happen.

## Non-goals (enforced)
- **NO order placement, signing, or CLOB credentials.** The legacy HMAC interceptor, order DTOs and `POST /order` code paths must be removed or avoided.
- **NO custody, no wallet keys.** Portfolio is by public address only.

## Stack
- Jetpack Compose + Material 3, single-activity, Navigation Compose.
- Kotlin coroutines + Flow end to end.
- Retrofit + Moshi (codegen) + OkHttp (REST), OkHttp WebSocket (live prices).
- Room (watchlist, recent searches), DataStore Preferences (settings).
- Coil (market imagery), custom Canvas charts (no chart dependency).
- Manual DI via `AppContainer` (no Hilt — kept deliberately simple).

## Data Sources (all public, keyless)
- **Gamma API**: `https://gamma-api.polymarket.com/` (events, markets, tags, search)
- **CLOB API (read-only)**: `https://clob.polymarket.com/` (order book, midpoints, price history)
- **Data API**: `https://data-api.polymarket.com/` (positions, activity, portfolio value, holders, trades)
- **Market WS**: `wss://ws-subscriptions-clob.polymarket.com/ws/market` (live `book` / `price_change` / `last_trade_price` events)
- **AI providers (BYO key)**: Gemini / xAI / OpenAI (research assistant, X sentiment, probability reasoning)

## Navigation & Architecture
- **Bottom bar**: Discover · Watchlist · Portfolio. Settings via top-bar gear.
- **Routing**: `market/{marketId}` is pushed from anywhere; `marketId` is the Gamma market id.
- **State Management**: ViewModels mapped to Repositories (e.g., `DiscoverViewModel` -> `MarketRepository`).
