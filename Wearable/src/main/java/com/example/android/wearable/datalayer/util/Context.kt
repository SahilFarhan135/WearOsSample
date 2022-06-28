package com.example.android.wearable.datalayer.util

import android.content.Context

object ContextApp {
    var appContext: Context?=null
    fun setContext(context:Context){
        appContext =context
    }
    fun getContext():Context{
        return appContext!!
    }
}