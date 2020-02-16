package com.sysu.example

import android.app.Application
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.StrPropertyHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        ContextApi.init(this)

        val strPropertyHelper = StrPropertyHelper(ConfigProperty::class.java)
        strPropertyHelper.readFromJson("http://192.168.43.47:3000/")
        ContextApi.global["StrPropertyHelper"] = strPropertyHelper
    }
}
