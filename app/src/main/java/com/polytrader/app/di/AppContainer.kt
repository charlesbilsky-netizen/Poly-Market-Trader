package com.polytrader.app.di

import android.content.Context
import com.polytrader.app.data.ai.AiGateway
import com.polytrader.app.data.db.PolyDatabase
import com.polytrader.app.data.remote.NetworkModule
import com.polytrader.app.data.remote.ws.MarketWebSocketClient
import com.polytrader.app.data.repo.MarketRepository
import com.polytrader.app.data.repo.PortfolioRepository
import com.polytrader.app.data.repo.ResearchRepository
import com.polytrader.app.data.repo.WatchlistRepository
import com.polytrader.app.data.settings.SettingsRepository
import com.polytrader.app.domain.model.AiProvider
import kotlinx.coroutines.runBlocking

/**
 * Hand-rolled DI graph. Everything is lazy; ViewModels receive dependencies
 * through their factories (see each screen's ViewModel companion).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository by lazy { SettingsRepository(appContext) }

    private val database by lazy { PolyDatabase.build(appContext) }

    val watchlistRepository by lazy {
        WatchlistRepository(database.watchlistDao(), database.recentSearchDao())
    }

    val marketRepository by lazy {
        MarketRepository(NetworkModule.gammaApi, NetworkModule.clobApi, NetworkModule.dataApi)
    }

    val portfolioRepository by lazy {
        PortfolioRepository(NetworkModule.dataApi, NetworkModule.gammaApi)
    }

    val aiGateway by lazy {
        AiGateway(NetworkModule.okHttpClient) { provider: AiProvider ->
            // Key lookups happen on background dispatchers inside AiGateway calls.
            runBlocking { settingsRepository.current().keyFor(provider) }
        }
    }

    val researchRepository by lazy { ResearchRepository(marketRepository, aiGateway) }

    /** One shared live-price socket, keyed by the screen currently using it. */
    fun newMarketSocket() = MarketWebSocketClient(NetworkModule.okHttpClient)
}
