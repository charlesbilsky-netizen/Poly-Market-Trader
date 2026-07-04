package com.polytrader.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.polytrader.app.di.AppContainer
import com.polytrader.app.ui.detail.MarketDetailScreen
import com.polytrader.app.ui.discover.DiscoverScreen
import com.polytrader.app.ui.portfolio.PortfolioScreen
import com.polytrader.app.ui.settings.SettingsScreen
import com.polytrader.app.ui.watchlist.WatchlistScreen

object Routes {
    const val DISCOVER = "discover"
    const val WATCHLIST = "watchlist"
    const val PORTFOLIO = "portfolio"
    const val SETTINGS = "settings"
    const val MARKET = "market/{marketId}"
    fun market(marketId: String) = "market/$marketId"
}

private data class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
fun AppNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        BottomTab(Routes.DISCOVER, "Discover") { Icon(Icons.Filled.Explore, null) },
        BottomTab(Routes.WATCHLIST, "Watchlist") { Icon(Icons.Filled.Star, null) },
        BottomTab(Routes.PORTFOLIO, "Portfolio") { Icon(Icons.Filled.AccountBalanceWallet, null) },
    )
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.DISCOVER) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = tab.icon,
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DISCOVER,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    container = container,
                    onOpenMarket = { navController.navigate(Routes.market(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.WATCHLIST) {
                WatchlistScreen(
                    container = container,
                    onOpenMarket = { navController.navigate(Routes.market(it)) },
                )
            }
            composable(Routes.PORTFOLIO) {
                PortfolioScreen(
                    container = container,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container, onBack = { navController.popBackStack() })
            }
            composable(Routes.MARKET) { entry ->
                val marketId = entry.arguments?.getString("marketId").orEmpty()
                MarketDetailScreen(
                    container = container,
                    marketId = marketId,
                    onBack = { navController.popBackStack() },
                    onOpenMarket = { navController.navigate(Routes.market(it)) },
                )
            }
        }
    }
}
