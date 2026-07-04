package com.polytrader.app

import android.app.Application
import com.polytrader.app.di.AppContainer

class PolyTraderApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
