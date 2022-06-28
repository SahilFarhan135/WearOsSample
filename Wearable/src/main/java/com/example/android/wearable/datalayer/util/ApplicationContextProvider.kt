package com.example.android.wearable.datalayer.util

import android.annotation.SuppressLint
import android.content.Context

class ApplicationContextProvider private constructor(private val context: Context) {
    var appContext: Context? = null

    init {
        appContext = context
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @get:Synchronized
        var instance: ApplicationContextProvider? = null
            private set

        fun initInstance(context: Context) {
            instance = ApplicationContextProvider(context)
        }
    }


}