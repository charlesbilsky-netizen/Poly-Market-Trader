package com.example

import android.app.Application
import android.content.Context

/**
 * Application singleton so plain [androidx.lifecycle.ViewModel]s (created via
 * `viewModel()` with no factory) can reach an application [Context] for Room
 * and other context-bound services without a DI framework.
 */
class PolyTraderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        @Volatile
        lateinit var appContext: Context
            private set
    }
}
