package com.sysu.example

import android.app.Application
import com.sysu.example.utils.ContextApi

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextApi.init(this)
    }
}
