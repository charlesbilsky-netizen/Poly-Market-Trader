# Polymarket Research Companion — API Reference (Android, Read-Only)

**Compiled 2026-07-04.** Synthesized from official docs (docs.polymarket.com, docs.x.com, docs.x.ai, ai.google.dev) and live API probes. Items verified live are noted; anything marked **UNVERIFIED** was not confirmed against official docs or live behavior — treat defensively.

**Base URLs (all public, no authentication):**

| Service | Base URL |
|---|---|
| Gamma API | `https://gamma-api.polymarket.com` |
| CLOB API (read) | `https://clob.polymarket.com` |
| Data API | `https://data-api.polymarket.com` |
| CLOB WebSocket | `wss://ws-subscriptions-clob.polymarket.com` |
| RTDS WebSocket | `wss://ws-live-data.polymarket.com` |
| Website (deep links) | `https://polymarket.com` |

Per [docs.polymarket.com/api-reference/authentication](https://docs.polymarket.com/api-reference/authentication): "The Gamma API, Data API, and CLOB read endpoints (orderbook, prices, spreads) require no authentication." Auth (L1 EIP-712 / L2 HMAC) is only for trading and user-channel WebSocket — irrelevant to a read-only app. Docs index: [docs.polymarket.com/llms.txt](https://docs.polymarket.com/llms.txt) (append `.md` to any doc URL for raw markdown).

---

## 1. Gamma API — markets, events, tags, search, comments, profiles

Base: `https://gamma-api.polymarket.com`. List endpoints return a **bare JSON array** (no envelope) unless noted. **Null fields are omitted from responses** — make every model field nullable.

### 1.1 GET /markets

Doc: [list-markets](https://docs.polymarket.com/api-reference/markets/list-markets). Returns `Market[]`.

| Param | Type | Notes |
|---|---|---|
| `limit`, `offset` | int | pagination |
| `order` | string | comma-separated fields, e.g. `volume24hr`, `liquidityNum`, `endDate`, `id` |
| `ascending` | bool | with `order` |
| `id` | int, repeatable | filter by market id(s) |
| `slug` | string, repeatable | filter by market slug(s) |
| `closed` | bool, default `false` | |
| `clob_token_ids` | string, repeatable | |
| `condition_ids` | string, repeatable | |
| `market_maker_address` | string, repeatable | |
| `liquidity_num_min` / `liquidity_num_max` | number | |
| `volume_num_min` / `volume_num_max` | number | |
| `start_date_min/max`, `end_date_min/max` | date-time | e.g. `2026-08-01T00:00:00Z` |
| `tag_id` | int | + `related_tags` (bool) to include related tags |
| `cyom`, `uma_resolution_status`, `game_id`, `sports_market_types`, `rewards_min_size`, `question_ids`, `include_tag` | misc | |

**Important:** `active` and `archived` are **NOT query params on /markets** — live-verified they are silently ignored. They exist only as response fields; filter client-side or use `closed`.

Related endpoints:
- `GET /markets/{id}` (integer id), `GET /markets/slug/{slug}`, get-market-by-token, `GET /markets/{id}/tags`
- `GET /markets/keyset` → `{"markets":[...], "next_cursor":"..."}`; pass cursor back as `after_cursor`; `limit` max 500, default 20; `offset` rejected with 422

### 1.2 Market object fields (exact JSON names, all nullable)

- **Identity:** `id` (string), `question`, `conditionId` (0x hex), `slug`, `questionID`, `description`, `category` (usually omitted), `groupItemTitle`, `groupItemThreshold`, `resolutionSource`, `resolvedBy`, `submitted_by`, `marketMakerAddress`
- **Outcomes (CRITICAL):** `outcomes`, `outcomePrices`, `clobTokenIds` — **all three are JSON-encoded STRINGS, not arrays** (live-verified), e.g. `"outcomes":"[\"Yes\", \"No\"]"`, `"outcomePrices":"[\"0.0025\", \"0.9975\"]"`. Requires a second JSON decode.
- **Prices/book (native numbers):** `bestBid`, `bestAsk`, `lastTradePrice`, `spread`, `oneDayPriceChange`, `oneHourPriceChange`, `oneWeekPriceChange`, `oneMonthPriceChange`, `oneYearPriceChange`, `orderPriceMinTickSize`, `orderMinSize`, `competitive`
- **Volume/liquidity:** `volume` (STRING), `liquidity` (STRING), `volumeNum` / `liquidityNum` (numbers — prefer these), `volume24hr`, `volume1wk`, `volume1mo`, `volume1yr` (numbers), plus `volumeClob`/`liquidityClob`/`volume24hrClob`/… and AMM equivalents
- **Dates (ISO 8601):** `endDate`, `startDate`, `createdAt`, `updatedAt`, `closedTime`, `endDateIso` / `startDateIso` (date-only, e.g. `"2026-07-20"`), `acceptingOrdersTimestamp`, `gameStartTime`, `eventStartTime`
- **Status booleans:** `active`, `closed`, `archived`, `new`, `featured`, `restricted`, `enableOrderBook`, `acceptingOrders`, `ready`, `funded`, `cyom`, `approved`, `rfqEnabled`, `feesEnabled`
- **UMA:** `umaBond` (string), `umaReward` (string), `umaResolutionStatus` (singular in schema; live objects often carry `umaResolutionStatuses` plural instead), `umaEndDate`
- **NegRisk:** `negRisk` (bool), `negRiskMarketID`, `negRiskRequestID`, `negRiskOther`
- **Media:** `image`, `icon` (URLs), `imageOptimized`/`iconOptimized` (objects)
- **Nesting:** `events` — array of full parent Event objects (source of `events[0].slug` for deep links and `events[0].id` for comments); also `tags` (Tag[]), `categories`
- **Fees:** `makerBaseFee`, `takerBaseFee` (numbers), `feeSchedule` (object), `feeType`

### 1.3 GET /events

Doc: [list-events](https://docs.polymarket.com/api-reference/events/list-events). Returns `Event[]`.

Params: `limit`, `offset`, `order`, `ascending`; `id` (int[]), `slug` (string[]), `tag_id` (int), `tag_slug` (string), `exclude_tag_id` (int[]), `related_tags` (bool); status bools `active`, `closed`, `archived`, `featured`, `cyom` (documented on /events, unlike /markets — but live testing suggests `active=false` may be ignored; treat false-value filtering as **UNVERIFIED**, use `closed=false&active=true` for "open"); metrics `liquidity_min/max`, `volume_min/max` (**no `_num_` on events**, unlike markets); dates `start_date_min/max`, `end_date_min/max`; also `recurrence`, `include_chat`, `include_template`.

Event fields: `id` (string), `ticker`, `slug`, `title`, `subtitle`, `description`, `resolutionSource`, `startDate`, `endDate`, `creationDate`, `createdAt`, `updatedAt`, `image`, `icon`, `active`, `closed`, `archived`, `featured`, `restricted`, `new`, `liquidity` (**NUMBER on events**, string on markets), `volume` (NUMBER), `openInterest`, `volume24hr`, `volume1wk`, `volume1mo`, `volume1yr`, `liquidityClob`, `competitive`, `commentCount`, `negRisk`, `negRiskMarketID`, `enableNegRisk`, `negRiskAugmented`, `cyom`, `sortBy`, `showAllOutcomes`, `showMarketImages`, `enableOrderBook`, `live`, `ended`, `closedTime`, `markets` (full Market[] with all §1.2 fields), `tags` (Tag[]), `series`, `categories`.

Related:
- `GET /events/{id}`, `GET /events/slug/{slug}`, `GET /events/{id}/tags`
- `GET /events/pagination` → `{"data": Event[], "pagination": {"hasMore": bool, "totalResults": int}}` — use when you need totals
- `GET /events/keyset` → `{"events":[...], "next_cursor"}`; params `after_cursor`, `limit` (max 500, default 20), `order`, `ascending`, plus `closed`, `live`, `featured`, `title_search`, `liquidity_min/max`, `volume_min/max`, `tag_id`, `series_id`, `game_id`; `offset` rejected 422

### 1.4 GET /tags

Doc: [list-tags](https://docs.polymarket.com/api-reference/tags/list-tags). Params: `limit`, `offset`, `order`, `ascending`, `include_template` (bool), `is_carousel` (bool).

Tag fields: `id` (string), `label`, `slug`, `forceShow`, `forceHide`, `isCarousel`, `publishedAt`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`. Examples: `{"id":"1","label":"Sports","slug":"sports"}`, `{"id":"2","label":"Politics","slug":"politics"}`.

- `GET /tags/{id}`, `GET /tags/slug/{slug}`
- `GET /tags/{id}/related-tags` → relationship objects `{id, tagID, relatedTagID, rank}`; `GET /tags/{id}/related-tags/tags` → full Tag objects; slug variants exist
- Category filtering: pass `tag_id=<id>` (+ `related_tags=true`) to /markets or /events; `tag_slug=` works on /events

### 1.5 GET /public-search

Doc: [search-markets-events-and-profiles](https://docs.polymarket.com/api-reference/search/search-markets-events-and-profiles).

Params: `q` (**required**), `limit_per_type`, `page`, `events_status` (e.g. `active`), `events_tag` (string[]), `keep_closed_markets` (int), `sort`, `ascending`, `search_tags` (bool), `search_profiles` (bool), `recurrence`, `exclude_tag_id` (int[]), `cache`, `optimized`.

Response is an **OBJECT** (not array): `{"events": Event[] (with nested markets), "tags": Tag[] (if search_tags=true), "profiles": [] (if search_profiles=true), "pagination": {"hasMore", "totalResults"}}`. There is **no separate `markets` key** — markets are found via their parent events.

### 1.6 GET /comments

Doc: [list-comments](https://docs.polymarket.com/api-reference/comments/list-comments).

```
GET /comments?parent_entity_type=Event&parent_entity_id=<eventId>&limit=40&offset=0&order=createdAt&ascending=false&get_positions=true&holders_only=false
```

- `parent_entity_type` + `parent_entity_id` are effectively **required together** (omitting → validation error). Live enum: `Event`, `Series`, `PerpsAsset` — docs say `market` but the live API **rejects it**; for a market's comments use its parent event id (`market.events[0].id`).
- `get_positions=true` adds commenter positions; `holders_only=true` filters to holders.
- Response fields: `id`, `body`, `parentEntityType`, `parentEntityID` (number), `userAddress`, `createdAt`, `updatedAt`, `profile` (`name`, `pseudonym`, `displayUsernamePublic`, `bio`, `proxyWallet`, `baseAddress`, `profileImage`, `positions[]` with `tokenId` and raw 1e6-scaled string `positionSize`), `reportCount`, `reactionCount`. Replies carry `parentCommentID`.
- Related: `GET /comments/{comment_id}`, `GET /comments/user_address/{address}?limit=…`. Spam is common — consider filtering on `reportCount`.

### 1.7 GET /public-profile

Doc: [get-public-profile-by-wallet-address](https://docs.polymarket.com/api-reference/profiles/get-public-profile-by-wallet-address).

```
GET https://gamma-api.polymarket.com/public-profile?address=0x...
```

`address` = proxy wallet or user address. Response: `createdAt`, `proxyWallet`, `profileImage`, `displayUsernamePublic`, `bio`, `pseudonym`, `name`, `xUsername`, `verifiedBadge`, `users[]` (`id`, `creator`, `mod`). 404 → `{"type":"not found error","error":"profile not found"}`.

---

## 2. CLOB API — read-only (books, prices, markets)

Base: `https://clob.polymarket.com` (staging: `clob-staging.polymarket.com`). All endpoints below are public — verified with plain curl, no headers.

### 2.1 Order book

**`GET /book?token_id={token_id}`** — [get-order-book](https://docs.polymarket.com/api-reference/market-data/get-order-book)

`token_id` = ERC-1155 token id (huge decimal string; NOT the 0x conditionId). Live-verified response — **all prices/sizes are strings**:

```json
{
  "market": "0x0e7b7cc2…",
  "asset_id": "43187333641…",
  "timestamp": "1783175805171",
  "hash": "f97e0d473ae2…",
  "bids": [{"price": "0.001", "size": "4638"}],
  "asks": [{"price": "0.999", "size": "2008.65"}],
  "min_order_size": "5",
  "tick_size": "0.001",
  "neg_risk": false,
  "last_trade_price": "0.982"
}
```

- **Ordering (live-verified, not in docs):** `bids` and `asks` are sorted worst→best — **best bid = LAST element of `bids`; best ask = LAST element of `asks`.** Do not assume best-first.
- Errors: 400 invalid token_id; 404 `{"error":"No orderbook exists for the requested token id"}`.

**`POST /books`** (batch) — body `[{"token_id":"…"}, …]`. Response is an array of the same OrderBook objects. **Response order is NOT guaranteed to match request order — join on `asset_id`** (live-verified).

### 2.2 Prices / midpoints / spreads

| Endpoint | Method | Params/Body | Live-verified response |
|---|---|---|---|
| `/price` | GET | `?token_id=…&side=BUY\|SELL` (both required) | `{"price":"0.018"}` — **string** (doc schema says number — wrong). `BUY` → best bid; `SELL` → best ask |
| `/prices` | POST | `[{"token_id":"…","side":"BUY"}, …]` (`side` optional → both) | `{"<token_id>":{"BUY":"0.018","SELL":"0.03"}}` |
| `/midpoint` | GET | `?token_id=…` | `{"mid":"0.024"}` — key is **`mid`** (doc says `mid_price` — wrong vs live) |
| `/midpoints` | POST | `[{"token_id":"…"}, …]` | `{"<token_id>":"0.024"}` |
| `/spread` | GET | `?token_id=…` | `{"spread":"0.012"}` |
| `/spreads` | POST | `[{"token_id":"…"}, …]` | `{"<token_id>":"0.012"}` |

**WARNING:** Documented GET query-param batch variants (`GET /prices?token_ids=a,b`, `GET /midpoints?token_ids=a,b`) return `{"error":"Invalid payload"}` live as of 2026-07-04 — **use the POST variants.**

Extras (same base, no auth): `GET /last-trade-price?token_id=`, `GET /tick-size?token_id=`, `GET /fee-rate` — shapes **UNVERIFIED** live.

### 2.3 CLOB markets endpoints

Paginated envelope (live-verified): `{"data":[…], "next_cursor":"MTAwMA==", "limit":1000, "count":1000}`. Cursor is base64 of the offset (start `MA==`); end marker `LTE=` ("-1") is long-standing convention but **UNVERIFIED in current docs** — stop when `next_cursor == "LTE="` or `data` is empty. Page size fixed at 1000.

- **`GET /markets?next_cursor=…`** — full Market objects, snake_case fields: `condition_id`, `question_id`, `question`, `description`, `market_slug`, `end_date_iso`, `game_start_time`, `seconds_delay`, `fpmm`, `maker_base_fee`, `taker_base_fee`, `minimum_order_size`, `minimum_tick_size`, `enable_order_book`, `active`, `closed`, `archived`, `accepting_orders`, `accepting_order_timestamp`, `notifications_enabled`, `neg_risk`, `neg_risk_market_id`, `neg_risk_request_id`, `icon`, `image`, `is_50_50_outcome`, `tags` (string[]), `rewards` (`{rates:[{asset_address, rewards_daily_rate}], min_size, max_spread}`), `tokens` (exactly 2: `[{token_id, outcome, price, winner}]` — `price` is a **number** here, `winner` boolean).
- **`GET /markets/{condition_id}`** — works live (single unwrapped Market), but absent from current api-reference index — docs status **UNVERIFIED**, behavior verified.
- **`GET /simplified-markets?next_cursor=…`** — slim shape: `condition_id`, `tokens`, `rewards`, `active`, `closed`, `archived`, `accepting_orders`.
- **`GET /sampling-markets`** / **`GET /sampling-simplified-markets`** — markets with active liquidity-rewards programs ("sampling" definition **UNVERIFIED** in current docs).

Note the schema split: CLOB uses snake_case (`condition_id`); Gamma uses camelCase (`conditionId`). Prefer Gamma for browsing (rich filters); CLOB for books/prices.

---

## 3. Price history — `GET /prices-history` (CLOB)

**The recommended way to chart historical probability.** Doc: [get-prices-history](https://docs.polymarket.com/api-reference/markets/get-prices-history).

```
GET https://clob.polymarket.com/prices-history?market=<clobTokenId>&interval=1d&fidelity=60
```

| Param | Type | Notes |
|---|---|---|
| `market` | string, **required** | **the token_id (CLOB asset id), NOT conditionId** — despite the param name. From gamma `clobTokenIds` (JSON-string array) |
| `startTs` / `endTs` | number, optional | unix **seconds** |
| `interval` | string, optional | `max`, `all`, `1m` (one **month**), `1w`, `1d`, `6h`, `1h` — trailing window ending now |
| `fidelity` | int, optional | resolution in **minutes**, default 1. Server minimums (live-verified): `1w` requires ≥5, `1m` requires ≥10 |

Response (exact, live-verified): `{"history":[{"t":1783090811,"p":0.028}, …]}` — `t` = unix seconds (number), `p` = price 0..1 (float **number**, not string — unlike other CLOB endpoints).

- When both `interval` and `startTs` are sent, interval wins (verified live, but precedence undocumented — **UNVERIFIED behavior**; prefer one or the other).
- Errors: HTTP 400, `{"error":"invalid filters: …","code":"…","retry_after_seconds":0}`.
- Caveat (**UNVERIFIED**, community-reported: py-clob-client#216): resolved/closed markets may only return coarse ~12h granularity.

**`POST /batch-prices-history`** — [doc](https://docs.polymarket.com/api-reference/markets/get-batch-prices-history). Body: `{"markets":["<token_id>", …], "start_ts":…, "end_ts":…, "interval":"1h", "fidelity":10}` — `markets` **max 20**; note **snake_case `start_ts`/`end_ts`** here vs camelCase on the GET. Response: `{"history":{"<token_id>":[{"t":…,"p":…}, …]}}`.

Do NOT use Goldsky subgraphs for charting — no price-timeseries entity, and live queries hit Postgres `statement timeout` errors. (Subgraph endpoints exist under Goldsky project `project_cl6mb8i9h0003e201j6li0diw` — orderbook, positions `0.0.7`, activity `0.0.4`, oi `0.0.6`, pnl `0.0.14` — but they are unsuitable for mobile: no SLA, versioned URLs can vanish (`/prod/gn` paths 404 for positions/activity), 1e6-scaled big-number strings. Use REST.)

---

## 4. WebSocket — CLOB market channel (public, no auth)

**URL:** `wss://ws-subscriptions-clob.polymarket.com/ws/market`
Docs: [market-channel](https://docs.polymarket.com/market-data/websocket/market-channel), [overview](https://docs.polymarket.com/market-data/websocket/overview), AsyncAPI: [docs.polymarket.com/asyncapi.json](https://docs.polymarket.com/asyncapi.json).
(User channel `wss://ws-subscriptions-clob.polymarket.com/ws/user` requires L2 auth and subscribes by condition ids — not needed for read-only.)

### 4.1 Subscribe (first frame after connect)

```json
{"assets_ids": ["<clob_token_id_1>", "<clob_token_id_2>"], "type": "market"}
```

Required: `assets_ids` (CLOB token IDs, **NOT condition IDs**), `type` (const `"market"`). Optional: `custom_feature_enabled` (bool, default false — enables `best_bid_ask`, `new_market`, `market_resolved`), `initial_dump` (bool, default true — initial `book` snapshot), `level` (int 1|2|3, default 2).

Dynamic re-subscription after connect:
```json
{"assets_ids": ["new_id"], "operation": "subscribe", "custom_feature_enabled": true}
{"assets_ids": ["id_to_remove"], "operation": "unsubscribe"}
```

### 4.2 Keepalive

Send the **text frame `PING` every 10 seconds**; server replies `PONG` (documented interval). Ignore literal `PONG` frames when parsing. In Ktor/OkHttp send a plain text `"PING"` on a timer; setting OkHttp `pingInterval` as well is harmless belt-and-braces.

### 4.3 Message types (discriminate on `event_type`; all values are JSON strings unless noted)

**`book`** — full snapshot, sent on subscribe and after book-affecting trades:
```json
{"event_type":"book","asset_id":"658186…","market":"0xbd31dc8a…","bids":[{"price":".48","size":"30"}],"asks":[{"price":".52","size":"25"}],"timestamp":"123456789000","hash":"0x0…"}
```
Prices may lack a leading zero (`".48"`) — use a lenient decimal parser.

**`price_change`** — book delta on order placement/cancellation. Changes are nested in a **`price_changes` array** (one message can carry multiple asset deltas; older flat `changes` shape is deprecated):
```json
{"event_type":"price_change","market":"0x5f65…","price_changes":[{"asset_id":"713210…","price":"0.5","size":"200","side":"BUY","hash":"5662…","best_bid":"0.5","best_ask":"1"}],"timestamp":"1757908892351"}
```
Item required: `asset_id, price, size, side (BUY|SELL), hash`; optional `best_bid`, `best_ask`. **`size` is the NEW total size at that price level; `"size":"0"` means the level was removed.**

**`tick_size_change`** — fired when price > 0.96 or < 0.04: `{"event_type":"tick_size_change","asset_id":…,"market":…,"old_tick_size":"0.01","new_tick_size":"0.001","timestamp":…}`.

**`last_trade_price`** — trade execution: `{"event_type":"last_trade_price","asset_id":…,"market":…,"price":"0.456","side":"BUY","size":"219.217767","fee_rate_bps":"0","timestamp":"1750428146322"}`; optional `transaction_hash`.

**With `custom_feature_enabled: true`:**
- `best_bid_ask`: `{"event_type":"best_bid_ask","market":…,"asset_id":…,"best_bid":"0.73","best_ask":"0.77","spread":"0.04","timestamp":…}`
- `new_market`: required `event_type, id, question, market, slug, assets_ids, outcomes, timestamp`; also `description`, `event_message {id,ticker,slug,title,description}`, `tags[]`, `condition_id`, `active` (bool), `clob_token_ids[]`, `sports_market_type`, `line`, `game_start_time`, `order_price_min_tick_size`, `group_item_title`, `taker_base_fee`, `fees_enabled` (bool), `fee_schedule {exponent, rate, taker_only, rebate_rate}`
- `market_resolved`: required `event_type, id, market, assets_ids, winning_asset_id, winning_outcome, timestamp`; also `question, slug, description, outcomes[], event_message, tags[]`

### 4.4 Parsing notes

- One frame may contain a **JSON array of events** (initial subscribe commonly returns `[{book…}, {book…}]`) — **UNVERIFIED against current docs** but long-observed; handle both `[` and `{` as first char.
- `timestamp` is a **string of epoch milliseconds**.
- Keep all numerics as `String` in DTOs; convert to `BigDecimal` at use sites.

### 4.5 RTDS (secondary): `wss://ws-live-data.polymarket.com`

Doc: [rtds](https://docs.polymarket.com/market-data/websocket/rtds). Streams comments, crypto prices (Binance `crypto_prices` / Chainlink `crypto_prices_chainlink`), equity prices. Keepalive: text `PING` every **5 seconds** (different from CLOB's 10s). Envelope: `{"action":"subscribe","subscriptions":[{"topic":…,"type":…,"filters":…}]}`; received messages: `{"topic","type","timestamp" (NUMBER ms — unlike CLOB), "payload":{}}`. Comments topic types: `comment_created`, `comment_removed`, `reaction_created`, `reaction_removed`. A trades/`activity` topic is reported by third parties but **not in official docs — UNVERIFIED, do not rely on it.**

---

## 5. Data API — positions, activity, trades, value, holders, leaderboard

Base: `https://data-api.polymarket.com`. Public, no auth. Types: `Address` = `^0x[a-fA-F0-9]{40}$`; `Hash64` = 0x + 64 hex (conditionId). Errors return `{"error":"<message>"}`.

**All endpoints take the user's PROXY WALLET address, not the EOA** — see §5.7.

### 5.1 GET /positions

Doc: [get-current-positions-for-a-user](https://docs.polymarket.com/api-reference/core/get-current-positions-for-a-user).

| Param | Type | Default | Notes |
|---|---|---|---|
| `user` | Address, **required** | — | proxy wallet |
| `market` | Hash64[], comma-sep | — | mutually exclusive with `eventId` |
| `eventId` | int[], comma-sep | — | |
| `sizeThreshold` | number | `1` | filters dust below this size |
| `redeemable` / `mergeable` | bool | `false` | |
| `title` | string | — | max 100 chars |
| `limit` | int | `100` | 0–500 |
| `offset` | int | `0` | 0–10000 |
| `sortBy` | enum | `TOKENS` | `CURRENT, INITIAL, TOKENS, CASHPNL, PERCENTPNL, TITLE, RESOLVING, PRICE, AVGPRICE` |
| `sortDirection` | enum | `DESC` | `ASC, DESC` |

Position fields: `proxyWallet`, `asset` (token id string), `conditionId`, `size`, `avgPrice`, `initialValue`, `currentValue`, `cashPnl` (unrealized), `percentPnl`, `totalBought`, `realizedPnl`, `percentRealizedPnl`, `curPrice`, `redeemable`, `mergeable`, `title`, `slug`, `icon`, `eventSlug`, `outcome`, `outcomeIndex`, `oppositeOutcome`, `oppositeAsset`, `endDate`, `negativeRisk`.

**GET /closed-positions** — params: `user` (req), `market`, `eventId`, `title`, `limit` (default 10, max 50), `offset` (0–100000), `sortBy` (`REALIZEDPNL` default; `TITLE, PRICE, AVGPRICE, TIMESTAMP`), `sortDirection`. Fields as above minus `currentValue`/`cashPnl`/`negativeRisk`, plus `timestamp` (int64). Also available: positions-for-market and combo-position endpoints (see llms.txt).

### 5.2 GET /activity

Doc: [get-user-activity](https://docs.polymarket.com/api-reference/core/get-user-activity).

Params: `user` (Address, **required**), `limit` (default 100, 0–500), `offset` (0–10000), `market` (Hash64[]) XOR `eventId` (int[]), `type` (array: `TRADE, SPLIT, MERGE, REDEEM, REWARD, CONVERSION, DEPOSIT, WITHDRAWAL, YIELD, MAKER_REBATE, TAKER_REBATE, REFERRAL_REWARD`), `start`/`end` (epoch seconds), `side` (`BUY|SELL`), `sortBy` (`TIMESTAMP` default; `TOKENS, CASH`), `sortDirection`.

Activity fields: `proxyWallet`, `timestamp` (int64 unix seconds), `conditionId`, `type`, `size` (tokens), `usdcSize` (USD), `transactionHash`, `price`, `asset`, `side`, `outcomeIndex`, `title`, `slug`, `icon`, `eventSlug`, `outcome`, plus profile decoration `name`, `pseudonym`, `bio`, `profileImage`, `profileImageOptimized`, and `isCombo` (bool).

### 5.3 GET /trades

Doc: [get-trades-for-a-user-or-markets](https://docs.polymarket.com/api-reference/core/get-trades-for-a-user-or-markets).

Params: `user` (optional here), `market` (comma-sep conditionIds) XOR `eventId`, `side` (`BUY|SELL`), `takerOnly` (bool, default **true** — taker fills only; false includes maker legs), `filterType` (`CASH|TOKENS`) + `filterAmount` (must be paired — e.g. `filterType=CASH&filterAmount=1000` for a whale-trades feed of trades ≥ $1000), `limit` (default 100, max 10000), `offset` (0–10000).

Trade fields: `proxyWallet`, `side`, `asset` (clobTokenId), `conditionId`, `size` (shares), `price` (0..1; notional USD = size×price), `timestamp` (unix seconds), `title`, `slug`, `icon`, `eventSlug`, `outcome`, `outcomeIndex`, `name`, `pseudonym`, `bio`, `profileImage`, `profileImageOptimized`, `transactionHash`. Denormalized market metadata means no join needed for a feed UI. No cursor — page by `offset`.

### 5.4 GET /value and GET /traded

- **`GET /value?user=0x…[&market=…]`** ([doc](https://docs.polymarket.com/api-reference/core/get-total-value-of-a-users-positions)) → **array** of `{"user": Address, "value": number}` — total current position value in USDC; **does not include cash balance**.
- **`GET /traded?user=0x…`** ([doc](https://docs.polymarket.com/api-reference/misc/get-total-markets-a-user-has-traded)) → `{"user": Address, "traded": int}`.

### 5.5 GET /holders

Doc: [get-top-holders-for-markets](https://docs.polymarket.com/api-reference/core/get-top-holders-for-markets).

```
GET /holders?market=<conditionId>[,<conditionId>…]&limit=20&minBalance=1
```

`market` **required** (conditionIds); `limit` default 20, **hard cap 20**; `minBalance` default 1, 0–999999.

Response: array of `{"token": "<clobTokenId>", "holders": Holder[]}` — one entry **per outcome token**. Holder: `proxyWallet`, `pseudonym`, `name`, `amount` (shares, human units), `asset`, `outcomeIndex`, `bio`, `displayUsernamePublic`, `profileImage`, `profileImageOptimized`, `verified`. Match `token` to gamma `clobTokenIds[i]` / `outcomeIndex` to label Yes/No. Private users (`displayUsernamePublic:false`) have empty `name`/`pseudonym`.

### 5.6 GET /v1/leaderboard

Doc: [get-trader-leaderboard-rankings](https://docs.polymarket.com/api-reference/core/get-trader-leaderboard-rankings).

```
GET https://data-api.polymarket.com/v1/leaderboard
```

| Param | Default | Values |
|---|---|---|
| `category` | `OVERALL` | `OVERALL, POLITICS, SPORTS, ESPORTS, CRYPTO, CULTURE, MENTIONS, WEATHER, ECONOMICS, TECH, FINANCE` |
| `timePeriod` | `DAY` | `DAY, WEEK, MONTH, ALL` |
| `orderBy` | `PNL` | `PNL, VOL` |
| `limit` | `25` | 1–50 |
| `offset` | `0` | 0–1000 |
| `user` / `userName` | — | filter to one trader (get their rank/stats) |

Response rows: `rank` (string), `proxyWallet`, `userName`, `vol` (number), `pnl` (number), `profileImage`, `xUsername`, `verifiedBadge` (bool).

Legacy host `lb-api.polymarket.com` (`/leaderboard?window=…&rankType=…`) is not in current docs — deprecated/**UNVERIFIED**; use `/v1/leaderboard`. A separate "User PNL API" is rate-limited at 200 req/10s per the rate-limits page; its host is **UNVERIFIED** (historically `user-pnl-api.polymarket.com`).

### 5.7 Proxy wallets — which address to pass

- Every Polymarket account trades through a **smart-contract proxy wallet on Polygon, distinct from the signing EOA**. The proxy holds all USDC and ERC-1155 outcome tokens; it's the address in `polymarket.com/profile/0x…`.
- **All Data API endpoints expect the proxy wallet.** Passing the EOA returns empty/zero results. Every response row echoes it as `proxyWallet`.
- Two proxy types: browser-wallet users → 1-of-1 Gnosis Safe via factory `0xaacfeea03eb1561c4e67d661e40682bd20e3541b` (sig type `POLY_GNOSIS_SAFE` = 2); Magic Link users → custom proxy via factory `0xaB45c5A4B0c941a2F231C04C3f49182e1A254052` (sig type `POLY_PROXY` = 1). CREATE2-deterministic from the owner EOA. New 2026 "deposit wallets" (`POLY_1271`, value 3) don't change read-only lookups.
- Resolution flow: username → `gamma-api /public-profile` (or leaderboard `userName` filter) → take `proxyWallet` → feed into `/positions`, `/activity`, `/value`, `/traded`.

---

## 6. Deep-link URL patterns (polymarket.com, live-verified 2026-07-04)

### 6.1 Event / market pages

| Pattern | Status | Notes |
|---|---|---|
| `https://polymarket.com/event/{event-slug}` | 200 | Canonical event page |
| `https://polymarket.com/event/{event-slug}/{market-slug}` | 200 | Single market within an event |
| `https://polymarket.com/market/{market-slug}` | 307 → `/event/{event-slug}/{market-slug}` | **Best target with only a market slug** — server resolves the parent event |
| `https://polymarket.com/event/{market-slug}` | 404 | Market slug does NOT work in event position |
| `https://polymarket.com/event/{event-slug}/{bogus}` | 200 (soft) | Bad market segment tolerated; bad event slug is a real 404 |
| `https://polymarket.com/markets` | 308 → `/predictions` | |

### 6.2 Profiles / leaderboard / search

| Pattern | Status | Notes |
|---|---|---|
| `https://polymarket.com/profile/{0xProxyWallet}` | 200 | Must be **proxy wallet address**; `/profile/{username}` → 404 |
| `https://polymarket.com/@{username}` | 200 | Username-handle profile route |
| `https://polymarket.com/leaderboard` | 200 | |
| `https://polymarket.com/leaderboard/{category}/{period}/{metric}` | 200 | Verified: `overall` / `weekly\|monthly\|all` / `profit\|volume`. Full category enum **UNVERIFIED** |
| `https://polymarket.com/predictions?q={query}` | 200 | Canonical search; `/search?q=` 308-redirects here |

### 6.3 Building URLs from Gamma data

- The Gamma market object has **no `eventSlug` field**; the parent slug is `market.events[0].slug`.
- Event link: `https://polymarket.com/event/{event.slug}`
- Market link (both slugs): `https://polymarket.com/event/{market.events[0].slug}/{market.slug}`
- Market link (market slug only): `https://polymarket.com/market/{market.slug}` (server 307s to canonical) — simplest robust option.

### 6.4 Native app interop

- Official Android app exists: package **`com.polymarket.android`** ("Polymarket", Google Play) — it is the **Polymarket US / CFTC-regulated** app, not the .com site.
- **`https://polymarket.com/.well-known/assetlinks.json` → 404** — polymarket.com publishes NO Digital Asset Links, so a `VIEW` intent to a polymarket.com URL opens the **browser**, never a verified App Link into the native app. Verified App Links exist only on `polymarket.us` (different URL scheme; .com event slugs are not portable).
- No custom scheme (e.g. `polymarket://`) found — **UNVERIFIED/none**. Plan for browser handoff.

### 6.5 Tracking params

- `?via={code}` — referral code, appendable to any polymarket.com URL. Param name confirmed by live acceptance + third-party sources but **not in official docs — UNVERIFIED-officially** (30-day attribution; 10%/5% fee rev-share per help center).
- `?tid={id}` — share-button tracking id; undocumented — **UNVERIFIED**.
- Standard `utm_*` params are ignored.

---

## 7. X sentiment options

### 7.1 X API v2 — direct (NOT recommended)

Since **2026-02-06** X replaced Free/Basic/Pro tiers with **pay-per-use credits** for all new developers — no free tier, credits must be purchased before any call ([docs.x.com pricing](https://docs.x.com/x-api/getting-started/pricing)).

Rates: **$0.005 per post read**, $0.010/user read, $0.001/likes-mutes-blocks resource, $0.015 post create (no link) / $0.200 (with URL), $0.001 "owned reads". A 2,000,000 post-reads/month cap is consistently reported by secondary sources but **UNVERIFIED on docs.x.com**. Legacy Basic ($200/mo) / Pro ($5,000/mo) survive only for pre-Feb-2026 subscribers, Basic auto-migrating from 2026-06-01 (**UNVERIFIED** — secondary sources only). Full-archive search remains Pro/Enterprise-only; 7-day recent search is available pay-per-use.

Endpoint ([recent-search](https://docs.x.com/x-api/posts/recent-search)):
```
GET https://api.x.com/2/tweets/search/recent
Authorization: Bearer <APP_BEARER_TOKEN>
```
Params: `query` (required, 1–4096 chars, operators e.g. `"fed rate cut" -is:retweet lang:en`), `max_results` (10–100, default 10), `start_time`/`end_time`, `since_id`/`until_id`, `sort_order` (`recency|relevancy`), `next_token`, `tweet.fields`, `expansions`, `user.fields`. Response: `{"data":[{id, text, author_id, created_at, public_metrics:{retweet_count, reply_count, like_count, quote_count, bookmark_count, impression_count}}], "includes":{"users":[]}, "meta":{result_count, newest_id, oldest_id, next_token}}`.

**Cost reality:** 100 posts/refresh = $0.50; 10 markets × 4 refreshes/day ≈ **$600/month** — untenable for a consumer app, and you'd still need an LLM pass for sentiment.

### 7.2 xAI Grok — `x_search` tool (RECOMMENDED)

The old Live Search API (`search_parameters` on `/v1/chat/completions`) is **retired** — requests return `410 Gone` (retirement ~2026-01-12; date **UNVERIFIED against docs.x.ai**, reported via langchain#33961 / cherry-studio#12647). Replacement: agentic server-side tools on **`POST https://api.x.ai/v1/responses`** (OpenAI Responses-compatible). Model: **`grok-4.3`** (1M context); older ids are legacy.

Request ([docs.x.ai x-search](https://docs.x.ai/developers/tools/x-search)):
```json
POST https://api.x.ai/v1/responses
Authorization: Bearer <XAI_API_KEY>

{
  "model": "grok-4.3",
  "input": [{"role": "user", "content": "Summarize the last 48h of X sentiment on 'Fed cuts rates in September'. Classify as bullish/bearish/mixed with a -1..1 score and cite posts."}],
  "tools": [{"type": "x_search", "from_date": "2026-07-02", "to_date": "2026-07-04", "allowed_x_handles": ["someanalyst"]}]
}
```
- `x_search` filters are **top-level keys on the tool object** (not nested): `allowed_x_handles` (≤20) XOR `excluded_x_handles` (≤20), `from_date`/`to_date` (`YYYY-MM-DD`), `enable_image_understanding`, `enable_video_understanding`. (Contrast: `web_search` nests domain filters under `"filters":{"allowed_domains":[…]}`, ≤5.)
- Response: top-level `citations` (flat URL list, always returned); inline `output[]` → `type:"message"` → `content[]` `type:"output_text"` blocks with `annotations` of `{"type":"url_citation","url":"https://x.com/…/status/…","title","start_index","end_index"}`.
- Pricing ([docs.x.ai pricing](https://docs.x.ai/developers/pricing)): `x_search` **$5/1,000 invocations** ($0.005/call); grok-4.3 tokens $1.25/$2.50 per 1M in/out. Model controls invocation count (typically 1–5). **Full per-market sentiment analysis ≈ $0.01–0.05.**

### 7.3 Gemini — Google Search grounding (fallback)

Docs: [google-search](https://ai.google.dev/gemini-api/docs/google-search), [pricing](https://ai.google.dev/gemini-api/docs/pricing). Use the `google_search` tool on generateContent (`google_search_retrieval` is 1.5-era legacy; a newer Interactions API `POST /v1beta/interactions` exists but generateContent is the practical Android/REST surface):

```json
POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
x-goog-api-key: <GEMINI_API_KEY>

{"contents":[{"parts":[{"text":"What is current sentiment on <market question>?"}]}], "tools":[{"google_search":{}}]}
```

Response grounding metadata (exact field names): `candidates[].groundingMetadata` → `webSearchQueries[]`, `searchEntryPoint.renderedContent`, `groundingChunks[].web.{uri,title}`, `groundingSupports[].{segment:{startIndex,endIndex,text}, groundingChunkIndices[], confidenceScores[]}`, `retrievalMetadata`.

Pricing: Gemini 2.5 — **1,500 grounded requests/day free**, then $35/1,000 grounded prompts; Gemini 3 — 5,000 prompts/month free, then $14/1,000 search queries. Tokens: `gemini-2.5-flash` $0.30/$2.50 per 1M.

**Caveat:** grounding searches **Google's web index, not X** — X posts are inconsistently indexed, so this is second-hand "web sentiment," not genuine per-post X signal.

### 7.4 Recommendation

**Primary: Grok `x_search` via `POST https://api.x.ai/v1/responses`** — ~10–50x cheaper per insight than raw X API ($0.01–0.05 vs $0.50+ per analysis); one BYO key from `console.x.ai`; single round trip does search + sentiment classification + real x.com post citations; no X developer-account/credit-purchase flow. Ask for JSON in the prompt (e.g. `{"stance":"bullish","score":0.4,"volume":"high","key_posts":[…]}`), keep `response.citations` for sources, cap `max_output_tokens` as a budget guardrail.

**Fallback: Gemini `google_search` grounding (`gemini-2.5-flash`)** — effectively free at consumer volume, but label it "web sentiment," not "X sentiment."

**Avoid: direct X API v2** unless a power user needs raw `public_metrics` for a quantitative signal and already has funded credits.

Suggested design: `SentimentProvider` strategy interface — `GrokXSearchProvider` (default), `GeminiGroundedProvider` (free fallback), `XApiRawProvider` (power users) — all BYO-key, stored in `EncryptedSharedPreferences`/Keystore.

---

## 8. Implementation notes for Android client

**Parsing gotchas**
1. **JSON-encoded string arrays (Gamma):** `outcomes`, `outcomePrices`, `clobTokenIds` on Market are strings containing JSON arrays — decode twice (`JSONArray(stringField)` / nested `Json.decodeFromString`).
2. **Absent-when-null (Gamma):** null fields are omitted entirely (`bestBid`, `category`, `spread`, `oneDayPriceChange`, `umaResolutionStatus` can be missing) — every DTO field nullable with defaults.
3. **String vs number inconsistencies:** Gamma Market `volume`/`liquidity` are strings (use `volumeNum`/`liquidityNum`); Gamma Event `volume`/`liquidity` are numbers. All CLOB `/book`, `/price(s)`, `/midpoint(s)`, `/spread(s)` values are strings; `/prices-history` `p` and CLOB markets `tokens[].price` are numbers. WebSocket market-channel values are strings; RTDS `timestamp` is a number.
4. **Doc-vs-live divergences:** `/midpoint` returns key `mid` (docs say `mid_price`); `/price` returns a string (docs say number); GET batch variants of `/prices`/`/midpoints` are broken — use POST. Trust the live shapes documented above.
5. **IDs:** clobTokenId is a huge decimal string — **never int64/Long, always String**. conditionId is 0x-hex. Gamma `id`s are JSON strings while `tag_id`/`eventId` query params are numeric. `/prices-history?market=` takes a **token_id despite the name**; `/holders` and `/trades` `market=` take conditionIds.
6. **Order books:** best bid/ask are the **LAST** elements of `bids`/`asks`. Batch `/books` response order ≠ request order — join on `asset_id`. WS prices may lack leading zeros (`".48"`).
7. **Timestamps:** CLOB WS `timestamp` = string epoch **ms**; `/prices-history` `t` and Data API `timestamp` = epoch **seconds**; RTDS = number ms.
8. Single-object Gamma GETs include a `$schema` key — configure parsers to ignore unknown keys (`ignoreUnknownKeys = true`).
9. Comments raw scaling: `positions[].positionSize` is a 1e6-scaled string.

**WebSocket keepalive**
10. CLOB market channel: send text frame `"PING"` every **10s**, expect `"PONG"` (ignore it in the message handler). RTDS: `"PING"` every **5s**. These are text frames, not protocol pings — a timer-driven send is required; OkHttp `pingInterval` alone is insufficient.
11. Handle frames that are JSON **arrays** of events as well as single objects (**UNVERIFIED in docs**, long observed).
12. Subscribe with **clobTokenIds** (`assets_ids`), not conditionIds.

**Proxy wallets**
13. Every Data API `user` param and `/profile/{addr}` deep link needs the **proxy wallet**, not the EOA — EOA queries return empty results. Resolve usernames via `gamma-api /public-profile?address=` or leaderboard `userName=` filter, then use the returned `proxyWallet`.

**Rate limits** (sliding 10s windows, Cloudflare; over-limit is throttled/queued, not rejected — but still back off on 429)
14. Gamma: 4,000 general; `/events` 500; `/markets` 300; markets+events combined 900; `/tags` 200; `/public-search` 350. CLOB: 9,000 general; `/book`/`/price`/`/midpoint` 1,500 each; `/books`/`/prices`/`/midpoints` 500 each; `/prices-history` 1,000; tick-size 200 (`/spread` limits not itemized — **UNVERIFIED**, assume general). Data API: 1,000 general; `/trades` 200; `/positions` 150; `/closed-positions` 150. Platform-wide 15,000. Per-IP vs per-key basis unspecified. All far above a mobile client's needs — still cache aggressively and use exponential backoff honoring `retry_after_seconds` when present.

**Pagination**
15. Gamma: `limit`/`offset`, or keyset (`after_cursor`, max 500, rejects `offset` with 422); `/events/pagination` for total counts. CLOB `/markets`: fixed 1000/page, base64 `next_cursor`, stop at `"LTE="` or empty `data` (end marker **UNVERIFIED** in current docs). Data API `/trades`: offset only, no cursor.

**Deep links / app interop**
16. polymarket.com has **no assetlinks.json** — links open the browser; no known custom scheme. Use `/market/{slug}` when you only have a market slug (307s to canonical); build event URLs from `market.events[0].slug`.
17. `/prices-history` max batch is 20 tokens (`POST /batch-prices-history`, snake_case `start_ts`/`end_ts`); `fidelity` minimums: ≥5 for `1w`, ≥10 for `1m`. Resolved markets may return only coarse (~12h) history (**UNVERIFIED**, community-reported).

**Data caveats**
18. `/holders` `limit` hard-caps at 20 per token. `/value` excludes cash balance. `/trades` `takerOnly` defaults true. Gamma `/comments` rejects `parent_entity_type=market` — use the parent Event id; filter spam via `reportCount`. `/markets?active=&archived=` are silently ignored — filter client-side. Avoid Goldsky subgraphs on mobile (timeouts, no SLA, disappearing versioned URLs).

**Sentiment keys**
19. All sentiment providers are BYO-key: store in `EncryptedSharedPreferences`/Android Keystore; never ship bundled keys. Grok tool-invocation count is model-controlled — set `max_output_tokens` and surface estimated cost per analysis to the user.
