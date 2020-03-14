package com.sysu.example

import android.app.Application
import com.sysu.example.activity.ConfigProperty
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.StrPropertyHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        ContextApi.init(this)

        val strPropertyHelper = StrPropertyHelper(ConfigProperty::class.java)
        strPropertyHelper.readFromJson("$BASE_URL/config")
        ContextApi.global["StrPropertyHelper"] = strPropertyHelper
    }

    companion object {
        const val BASE_URL = "http://192.168.43.47:5000"
    }
}
