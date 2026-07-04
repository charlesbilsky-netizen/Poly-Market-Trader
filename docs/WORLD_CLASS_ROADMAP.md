# PolyTrader — World-Class Benchmark Report & Roadmap

*Synthesized July 4, 2026 from a 48-agent deep-research sweep (web search → source
fetch → adversarial verification). Claims marked ✅ survived 3-vote adversarial
verification against primary sources; claims marked ◑ are sourced but were not
fully re-verified (verification stage was cut short by session limits).*

---

## Part 1 — Where PolyTrader already clears the bar

✅ The strongest existing free Android Polymarket companion, **PolymarketViewer**
(open-source, Kotlin/Compose/M3), offers only: browse/search/filter/sort,
watchlists, event details with volume, price-history charts, threaded comments,
and widgets ([github.com/Streamatico/PolymarketViewer](https://github.com/Streamatico/PolymarketViewer)).

✅ It has **no price alerting, no order-book depth, no whale tracking, no
portfolio/PnL analytics** — verified 3-0 against its own documentation.

**PolyTrader v2.1 already ships beyond that baseline:** real CLOB history charts
with scrubbing, cumulative order-book depth, whale feed + background whale
alerts, multi-LLM research + Alpha deep scans, daily AI digest notifications,
correlation matrix, offline research library, read-only portfolio hooks. The
gap features below are what separates "better than the field" from
"world-class."

## Part 2 — Ranked roadmap (evidence → feasibility)

### 1. Play Store compliance positioning ⚠️ *do before/at submission*
◑ Google Play allows real-money prediction-market **operators** only via a
limited pilot restricted to CFTC-registered entities
([policy](https://support.google.com/googleplay/android-developer/answer/16902027)), and its
gambling policy warns non-gambling apps against providing gambling "support or
companion functionality" or **directing users to gambling services**
([policy](https://support.google.com/googleplay/android-developer/answer/9877032)).
**Action:** position the listing strictly as an *informational market-data and
research* app (like a stocks screener); avoid the words "trade/trading" in
listing copy and screenshots; consider renaming the in-app CTA from "OPEN IN
POLYMARKET" to neutral "VIEW ON POLYMARKET.COM"; complete the gambling
questionnaire honestly (informational, no wagering). **Feasibility: copy-only
change, 1 hour. This is the #1 distribution risk.**

### 2. Price alerts (user-set thresholds)
◑ TradingView's entire paywall ladder is built on alert quotas (3 free →
1,000 Ultimate) — alerting is the single most monetizable retention feature in
market apps ([tradingview.com/pricing](https://www.tradingview.com/pricing/)).
**Action:** per-market "alert me when probability crosses X%" on watchlisted
markets; the 6h WhaleWatchWorker cadence extends naturally with a 15-min
alert-check worker. **Feasibility: high — workers, Room, notifications all
exist.**

### 3. Calibration & accuracy analytics (mirror polymarket.com/accuracy)
✅ Polymarket officially publishes time-horizon accuracy (90% at 1 month →
98.5% at 4 hours, Brier 0.0627) with a replicable methodology: price snapshots
of resolved markets at fixed pre-resolution intervals
([polymarket.com/accuracy](https://polymarket.com/accuracy)). **Action:** ship a
"Base Rates" panel per category — how often favorites at this price actually
won — computed from resolved markets + `/prices-history`. No other companion
has it. **Feasibility: medium — public data only, ~1 day.**

### 4. Home-screen widgets
✅ The free competitor ships widgets; PolyTrader doesn't yet. A watchlist
probability widget (Glance API) keeps the app on the user's home screen daily.
**Feasibility: medium.**

### 5. "Why did this move?" one-tap AI explanations
◑ Robinhood's Cortex Digests (plain-language AI explanations of price moves,
reportedly ~1M users) define the 2026 AI-copilot bar, with the assistant
✅ verified as embedded app-wide and gated behind Gold ($5/mo)
([robinhood.com newsroom](https://robinhood.com/us/en/newsroom/robinhood-presents-yes-no-event/)).
**Action:** a "Why?" chip next to big 24h deltas → one-shot AI explanation using
the market's news context. **Feasibility: high — one prompt + existing AI chain.**

### 6. Trader PnL leaderboards & wallet profiles
◑ Dedicated tools (Predicting Top, PolyWallet) exist solely for trader
leaderboards and deep wallet analysis — proven demand
([Awesome-Prediction-Market-Tools](https://github.com/aarora4/Awesome-Prediction-Market-Tools)).
**Action:** Leaderboard tab via public `data-api /v1/leaderboard` + tappable
trader profiles (positions, PnL) — pure read-only. **Feasibility: high.**

### 7. Real-time streaming prices in-app
Best-in-class apps treat live tick UX as table stakes; PolyTrader's WebSocket
client exists but the UI currently refreshes on demand. **Action:** stream
`price_change` events into open market views with subtle pulse animations.
**Feasibility: medium — client code already written.**

### 8. Real portfolio PnL view
◑ Delta (read-only tracker, millions of users) reserves real-time quotes and
PnL for its top tier — PnL analytics is the highest-value screen in companion
apps ([delta.app](https://delta.app/academy/post/introducing-delta-pro-pro-more-power-more-choice)).
**Action:** wire the Portfolio tab to `data-api /positions` (already in
NetworkModule) for positions, cost basis, unrealized PnL, redeemable flags by
public address. **Feasibility: high.**

### 9. Freemium architecture (future monetization)
◑ Converging evidence: Robinhood gates AI at $5/mo ✅; Delta gates AI +
analytics at $4.49/mo; TradingView gates alerts/charts up to $199/mo.
**Action:** keep BYO-API-key AI free forever (it costs you nothing) and
reserve *hosted* AI (your key), higher alert quotas, and advanced analytics
for a future Pro tier. **Feasibility: architectural decision now, billing later.**

### 10. Cross-platform odds comparison (Polymarket × Kalshi)
◑ Oddpool already aggregates odds/liquidity/arbitrage across Polymarket and
Kalshi — the "Bloomberg terminal for prediction markets" positioning is
validated. **Action (later):** Kalshi public API comparison view for shared
events. **Feasibility: larger lift; post-launch.**

### 11. X (Twitter) sentiment via Grok `x_search`
From the Phase-0 API research: Grok's server-side X search returns cited posts
for ~$0.01–0.05/query — the only first-party X sentiment channel; Gemini
grounding is the free "web sentiment" fallback. **Action:** per-market
sentiment card with score, quotes, citations. **Feasibility: high — AI chain
and prompt patterns already in the app.**

### 12. Material 3 polish pass
Predictive-back, tablet/landscape layouts, `Glance` widget theming, dynamic
color opt-in, per-component a11y labels — the finish that reads "world-class"
in Play featuring reviews. **Feasibility: ongoing.**

---

*Method note: 106 agents launched; search/fetch/extract completed (48 agents,
1.5M tokens); 6 claims fully adversarially verified, 19 sourced claims verified
by single-source fetch only (marked ◑) because the verification fleet hit the
session usage limit. Re-run `deep-research` post-reset to harden ◑ claims if
needed.*
