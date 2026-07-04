package com.polytrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.polytrader.app.ui.nav.AppNavHost
import com.polytrader.app.ui.theme.PolyTraderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PolyTraderApp).container
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsState(initial = null)
            PolyTraderTheme(darkTheme = settings?.darkTheme ?: true) {
                AppNavHost(container = container)
            }
        }
    }
}
