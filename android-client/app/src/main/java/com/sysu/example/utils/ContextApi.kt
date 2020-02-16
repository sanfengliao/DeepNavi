package com.sysu.example.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object ContextApi {
    lateinit var app: Application
    lateinit var appContext: Context
    var activity: Activity? = null
    var fragment: Fragment? = null
    val activityStack: MutableList<Activity> = mutableListOf()
    val fragmentStack: MutableList<Fragment> = mutableListOf()

    val global: MutableMap<String, Any> = mutableMapOf()

    fun init(application: Application) {
        this.app = application
        this.appContext = application
        this.app.registerActivityLifecycleCallbacks(object : LogActivityLifecycleCallbacks() {
            private val fragmentLifecycleCallbacks = object : LogFragmentLifecycleCallbacks() {
                // TODO()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                this@ContextApi.activityStack.add(activity)
                if (activity is FragmentActivity) {
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                this@ContextApi.activity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                this@ContextApi.activity = null
            }

            override fun onActivityDestroyed(activity: Activity) {
                this@ContextApi.activityStack.remove(activity)
                if (activity is FragmentActivity) {
                    activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
                }
            }
        })
    }
}
