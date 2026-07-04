// PolyTrader App - Gemini AI Assistant Instructions

You are building PolyTrader, a mobile Android app using Jetpack Compose and Kotlin. 
Please adhere strictly to the following technical and product rules when assisting with this project:

# 1. Product Scope & Safety Constraints
- **Read-Only / Research Only**: PolyTrader is a research companion, NOT an execution platform. Do NOT attempt to integrate wallet signing, private keys, or write operations to the Polymarket CLOB.
- **Redirection**: For any actual trades, always deep-link the user to the Polymarket website via an Intent (e.g., `https://polymarket.com/event/...`).
- **Fake/Mock Data**: If you need placeholder data to prototype the UI before the Gamma/CLOB API is fully connected, label it clearly, but strive to replace it with real OkHttp/Retrofit REST calls as soon as possible.

# 2. Tech Stack & Architecture
- **UI Toolkit**: Jetpack Compose exclusively. Avoid XML layouts unless absolutely necessary.
- **Styling**: Strictly adhere to the custom `QuantTheme` defined in the project (slate dark backgrounds, vivid blue/green accents, monospace fonts for data). Avoid generic Material colors.
- **Dependency Injection**: Use manual constructor DI (`AppContainer` pattern). Do NOT introduce Hilt or Dagger. Keep the build lightweight.
- **Networking**: Use Retrofit + Moshi for REST calls and OkHttp for WebSockets. 

# 3. Code Style & Components
- **Components**: Use Material 3 standard components (`Card`, `OutlinedTextField`, `Scaffold`, `NavigationBar`).
- **UI Clarity**: Always leave generous padding around elements (e.g., `16.dp` for container padding).
- **Icons**: Use `Icons.Default` or `Icons.AutoMirrored` standard Material symbols (e.g., `Icons.Default.OpenInNew`). Do not import heavy external SVG dependencies without reason.
- **State Management**: Use Kotlin `StateFlow` in ViewModels and collect them using `collectAsStateWithLifecycle()` in Compose.

# 4. Error Handling
- Never let the app crash on network failures. Always wrap API calls in `try/catch` and expose UI states (Loading, Success, Error).
