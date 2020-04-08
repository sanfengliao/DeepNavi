package com.sysu.example.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object ContextApi {
    lateinit var app: Application
    lateinit var appContext: Context
    lateinit var handler: Handler
    var activity: Activity? = null
    var fragment: Fragment? = null
    val activityStack: MutableList<Activity> = mutableListOf()
    val fragmentStack: MutableList<Fragment> = mutableListOf()

    val global: MutableMap<String, Any> = mutableMapOf()

    fun init(application: Application) {
        this.app = application
        this.appContext = application
        handler = Handler(Looper.getMainLooper())
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

    fun toast(msg: String) = ContextApi.handler.post { Toast.makeText(ContextApi.appContext, msg, Toast.LENGTH_LONG).show() }
}

fun <T> returnToast(text: String, result: T? = null, duration: Int = Toast.LENGTH_LONG): T? {
    Toast.makeText(ContextApi.appContext, text, duration).show()
    return result
}

fun <T> returnToast2(text: String, result: T, duration: Int = Toast.LENGTH_LONG): T = returnToast(text, result, duration)!!
fun returnToast3(text: String, duration: Int = Toast.LENGTH_LONG): Unit = returnToast(text, Unit, duration)!!
