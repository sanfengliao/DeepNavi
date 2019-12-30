package com.sysu.example.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

open class EmptyActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
}

open class LogActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    companion object {
        private const val TAG = "ActivityLifecycle"
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "onActivityPaused: $activity")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "onActivityStarted: $activity")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "onActivityDestroyed: $activity")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d(TAG, "onActivitySaveInstanceState: $activity")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "onActivityStopped: $activity")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "onActivityCreated: $activity")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "onActivityResumed: $activity")
    }
}

// AnimatorListenerAdapter : Animator.AnimatorListener, Animator.AnimatorPauseListener
// FragmentManager.FragmentLifecycleCallbacks

open class LogFragmentLifecycleCallbacks : FragmentManager.FragmentLifecycleCallbacks() {
    companion object {
        private const val TAG = "FragmentLifecycle"
    }

    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onFragmentViewCreated fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentStopped fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Log.d(TAG, "onFragmentCreated fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentResumed fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Log.d(TAG, "onFragmentAttached fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentPreAttached(fm: FragmentManager, f: Fragment, context: Context) {
        Log.d(TAG, "onFragmentPreAttached fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentDestroyed fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentSaveInstanceState(fm: FragmentManager, f: Fragment, outState: Bundle) {
        Log.d(TAG, "onFragmentSaveInstanceState fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentStarted fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentViewDestroyed fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentPreCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Log.d(TAG, "onFragmentPreCreated fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentActivityCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        Log.d(TAG, "onFragmentActivityCreated fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentPaused fragmentManager: $fm, fragment: $f")
    }

    override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
        Log.d(TAG, "onFragmentDetached fragmentManager: $fm, fragment: $f")
    }
}

open class EmptyAnimationListener : Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation?) = Unit
    override fun onAnimationEnd(animation: Animation?) = Unit
    override fun onAnimationStart(animation: Animation?) = Unit
}
