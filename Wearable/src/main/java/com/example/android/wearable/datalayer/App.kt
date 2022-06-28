package com.example.android.wearable.datalayer

import android.app.Application
import com.example.android.wearable.datalayer.util.ApplicationContextProvider
import com.example.android.wearable.datalayer.util.ContextApp

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        ContextApp.setContext(this)
        ApplicationContextProvider.initInstance(this)
    }
}