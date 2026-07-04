# PolyTrader Next Steps - Advanced Agent Prompt

**Context:** PolyTrader is a Jetpack Compose Android application acting as a research-first companion for Polymarket. It currently features live WebSocket price feeds, Gamma/CLOB API integration, and AI-driven news sentiment analysis using Gemini/Grok/OpenAI. 

**Your Objective:** Continue the enhancement of PolyTrader by implementing the remaining core product pillars while strictly adhering to the architectural constraints.

**Immediate Priorities for Implementation:**

1. **Local Watchlist Persistence (Room Database)**
   - Implement a Room Database to store the user's "Watchlist" of markets locally.
   - The user should be able to toggle a "star" icon on any market card to save it.
   - Create a `WatchlistViewModel` and integrate it into the `WatchlistView` tab.
   - *Constraint:* Use Kotlin Coroutines Flow to observe the database in real-time. Use the `room-database-integration` skill.

2. **Advanced Price History Charting**
   - Enhance the `MarketDetailView` or `StandardOpportunityCard` with a custom Compose `Canvas` chart.
   - Fetch historical prices from the Polymarket CLOB API (`GET /prices-history`).
   - Draw a smooth path line chart with gradient fills beneath the curve, utilizing `QuantTheme` colors (e.g., `accentBlue` or `accentGreen` based on current delta).
   - *Constraint:* Do NOT use third-party charting libraries. Use raw Compose Canvas for high performance.

3. **PDF Research Report Export**
   - Implement the "exportable branded PDF research reports" feature mentioned in the product specs.
   - Aggregate the AI Analysis, News Sentiment, and Quant Analysis for a specific market and render it into a PDF document using Android's native `PdfDocument` API.
   - Provide a standard Android `Intent.ACTION_SEND` share sheet to let the user save or share the generated PDF.

4. **Advanced Gamma API Filtering & Search**
   - Upgrade the `DiscoverView` to support Gamma API filters: Trending, New, Ending Soon, and Category filtering.
   - Implement a robust full-text search bar that debounces input and queries the Gamma API endpoints with search parameters.

**Execution Guidelines:**
- **Zero Execution:** Never implement wallet signing or order placement. Redirect to `polymarket.com/event/...` for actual trading.
- **UI/UX Polish:** Maintain the `QuantTheme` (dark slate background, monospace fonts, vivid accents). Ensure minimum 48dp touch targets and use Material 3 M3 standards.
- **Read-Modify-Write:** Always `view_file` before editing any existing files. Avoid overwriting entire files; use targeted `edit_file` or `multi_edit_file` calls.

**Begin by reviewing `AGENTS.md` and `GEMINI.md`, then tackle Priority 1 (Room Database Watchlist).**
