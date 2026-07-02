package com.vkard.pro

import android.app.Application
import com.vkard.pro.di.AppContainer

class VKardApplication : Application() {
    lateinit var container: AppContainer
        private set
        
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
