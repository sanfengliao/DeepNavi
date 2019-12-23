@file:Suppress("MemberVisibilityCanBePrivate")

package com.sysu.example.block

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

interface ActivityInter {
    fun getFragment(): Fragment?
    fun getActivity(): Activity?
    fun getFragmentManager(): FragmentManager?
    fun getFragmentActivity(): FragmentActivity?

    fun finish()
    fun startActivity(intent: Intent)
    fun startActivityForResult(intent: Intent, requestCode: Int)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?)

    fun onNewIntent(intent: Intent)
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray)
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    fun onBackPressed()
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
}

open class ActivityProxy : ActivityInter {
    open var ai: ActivityInter? = null

    override fun getFragment(): Fragment? = ai?.getFragment()
    override fun getActivity(): Activity? = ai?.getActivity()
    override fun getFragmentManager(): FragmentManager? = ai?.getFragmentManager()
    override fun getFragmentActivity(): FragmentActivity? = ai?.getFragmentActivity()

    override fun finish() = ai?.finish() ?: Unit
    override fun startActivity(intent: Intent) = ai?.startActivity(intent) ?: Unit
    override fun startActivityForResult(intent: Intent, requestCode: Int) =
            ai?.startActivityForResult(intent, requestCode) ?: Unit

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) =
            ai?.startActivityForResult(intent, requestCode, options) ?: Unit

    override fun onNewIntent(intent: Intent) = Unit
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) = Unit
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) = Unit
    override fun onBackPressed() = Unit  // TODO
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = false

    companion object {
        fun of(activity: Activity) = if (activity is FragmentActivity) ActivityProxyImpl2(activity) else ActivityProxyImpl1(activity)
        fun of(fragmentActivity: FragmentActivity) = ActivityProxyImpl2(fragmentActivity)
        fun of(fragment: Fragment) = ActivityProxyImpl3(fragment)
    }
}

open class ActivityProxyImpl1(private val activity: Activity) : ActivityProxy() {
    override fun getFragment(): Fragment? = null
    override fun getActivity(): Activity? = activity
    override fun getFragmentManager(): FragmentManager? = if (activity is FragmentActivity) activity.supportFragmentManager else null
    override fun getFragmentActivity(): FragmentActivity? = if (activity is FragmentActivity) activity else null

    override fun finish() = activity.finish()
    override fun startActivity(intent: Intent) = activity.startActivity(intent)
    override fun startActivityForResult(intent: Intent, requestCode: Int) = activity.startActivityForResult(intent, requestCode)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) =
            activity.startActivityForResult(intent, requestCode, options)
}

open class ActivityProxyImpl2(private val fragmentActivity: FragmentActivity) : ActivityProxy() {
    override fun getFragment(): Fragment? = null
    override fun getActivity(): Activity? = fragmentActivity
    override fun getFragmentManager(): FragmentManager? = fragmentActivity.supportFragmentManager
    override fun getFragmentActivity(): FragmentActivity? = fragmentActivity

    override fun finish() = fragmentActivity.finish()
    override fun startActivity(intent: Intent) = fragmentActivity.startActivity(intent)
    override fun startActivityForResult(intent: Intent, requestCode: Int) = fragmentActivity.startActivityForResult(intent, requestCode)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) =
            fragmentActivity.startActivityForResult(intent, requestCode, options)
}

open class ActivityProxyImpl3(private val fragment: Fragment) : ActivityProxy() {
    override fun getFragment(): Fragment? = fragment
    override fun getActivity(): Activity? = fragment.activity
    override fun getFragmentManager(): FragmentManager? = fragment.activity?.supportFragmentManager
    override fun getFragmentActivity(): FragmentActivity? = fragment.activity

    override fun finish() = fragment.activity?.finish() ?: Unit
    override fun startActivity(intent: Intent) = fragment.startActivity(intent)
    override fun startActivityForResult(intent: Intent, requestCode: Int) = fragment.startActivityForResult(intent, requestCode)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) =
            fragment.startActivityForResult(intent, requestCode, options)
}

interface ActivityLifeCycleInter {
    companion object {
        const val ORIGINAL = 0
        const val STATE_CREATE = 1
        const val STATE_START = 2
        const val STATE_RESUME = 3
        const val STATE_PAUSE = 4
        const val STATE_STOP = 5
        const val STATE_DESTROY = 6
        const val STATE_RESTART = 7
        const val STATE_SAVE_INSTANCE_STATE = 8
        const val STATE_RESTORE_INSTANCE_STATE = 9
    }

    fun onCreate(bundle: Bundle?)
    fun onRestart()
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroy()

    fun onSaveInstanceState(bundle: Bundle)
    fun onRestoreInstanceState(bundle: Bundle)
}

interface FragmentLifeCycleInter {
    companion object {
        const val ORIGIN = 0
        const val STATE_ATTACH = 1
        const val STATE_CREATE = 2
        const val STATE_CREATE_VIEW = 3
        const val STATE_ACTIVITY_CREATE = 4
        const val STATE_START = 5
        const val STATE_RESUME = 6
        const val STATE_PAUSE = 7
        const val STATE_STOP = 8
        const val STATE_DESTROY_VIEW = 9
        const val STATE_DESTROY = 10
        const val STATE_DETACH = 11
        const val STATE_SAVE_INSTANCE_STATE = 12
    }

    fun onAttach(context: Context)
    fun onCreate(bundle: Bundle?)
    fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View?
    fun onActivityCreated(bundle: Bundle?)
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroyView()
    fun onDestroy()
    fun onDetach()

    fun onSaveInstanceState(bundle: Bundle)
}
