package com.sysu.example

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.sysu.example.KeyUrls.CONFIG
import com.sysu.example.activity.ConfigActivity.Companion.GET_CONFIG_FROM_NETWORK
import com.sysu.example.activity.ConfigProperty
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.StrPropertyHelper

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        ContextApi.init(this)

        val strPropertyHelper = StrPropertyHelper(ConfigProperty::class.java)
        if (GET_CONFIG_FROM_NETWORK.getData() == true || GET_CONFIG_FROM_NETWORK.getData() == null) {
            strPropertyHelper.readFromJson(CONFIG)
        }
        ContextApi.global["StrPropertyHelper"] = strPropertyHelper
        var task: Runnable? = null
        task = Runnable {
            if (GET_CONFIG_FROM_NETWORK.getData() == true || GET_CONFIG_FROM_NETWORK.getData() == null) {
                strPropertyHelper.readFromJson(CONFIG)
            }
            ContextApi.handler.postDelayed(task!!, GET_CONFIG_DURATION)
        }
        ContextApi.handler.postDelayed(task, GET_CONFIG_DURATION)
    }

    companion object {
        const val BASE_URL = "http://192.168.43.47:5000"
        const val GET_CONFIG_DURATION = 60 * 1000 * 1000L
    }
}
