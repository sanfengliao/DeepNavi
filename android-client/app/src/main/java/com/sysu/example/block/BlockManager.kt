package com.sysu.example.block

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

@Suppress("LeakingThis")
open class BlockManager(@LayoutRes layoutId: Int = 0) : BlockGroup(layoutId), FragmentLifeCycleInter, ActivityLifeCycleInter {
    companion object {
        const val KEY_FRAGMENT_STATE = "KEY_FRAGMENT_STATE"
        const val KEY_ACTIVITY_STATE = "KEY_ACTIVITY_STATE"
    }

    open var bundle: Bundle? = null
    override var blockManager: BlockManager?
        get() = this
        set(_) {}

    // init

    constructor(context: Context, @LayoutRes layoutId: Int = 0) : this(layoutId) {
        init(context)
    }

    constructor(blockManager: BlockManager, @LayoutRes layoutId: Int = 0) : this(layoutId) {
        init(blockManager.swb, blockManager.cwb, blockManager.rxHandler, null, this)
        context = blockManager.context
        inflater = blockManager.inflater
        provider = blockManager.provider
    }

    open fun initInActivity(activity: Activity): BlockManager {
        innerActivity = activity
        putData(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.ORIGINAL)
        return this
    }

    open fun initInFragment(fragment: Fragment): BlockManager {
        innerActivity = fragment.activity
        innerFragment = fragment
        putData(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.ORIGIN)
        return this
    }

    open fun initInBlockManager(blockManager: BlockManager): BlockManager {
        innerActivity = blockManager.innerActivity
        innerFragment = blockManager.innerFragment
        if (blockManager.getData(KEY_FRAGMENT_STATE) != null) {
            putData(KEY_FRAGMENT_STATE, blockManager.getData(KEY_FRAGMENT_STATE))
        }
        if (blockManager.getData(KEY_ACTIVITY_STATE) != null) {
            putData(KEY_ACTIVITY_STATE, blockManager.getData(KEY_ACTIVITY_STATE))
        }
        return this
    }

    // inflate / build

    override fun <T : View> setInflatedCallback(callback: (T) -> Unit): BlockManager {
        afterInflateListener = Runnable { callback(view as T) }
        return this
    }

    fun <T : View> setInflatedCallback2(callback: (T) -> Unit): BlockManager = setInflatedCallback<T>(callback)  // for java usage ???

    override fun onInflateView(context: Context, inflater: LayoutInflater, parent: ViewGroup?): View? {
        view = inflater.inflate(layoutId, null, false)
        parent?.addView(view)
        return view
    }

    open fun build() = build(null)
    override fun build(parent: ViewGroup?) = super.build(parent) as BlockManager
    override fun build(parent: ViewGroup?, index: Int?): BlockManager = super.build(parent, index) as BlockManager

    // activity proxy

    protected var innerActivity: Activity? = null
    protected var innerFragment: Fragment? = null
    override var ai: ActivityInter? = this

    // activity / fragment 的一些功能

    override fun getActivity(): Activity? = if (innerActivity == null && innerFragment != null) {
        innerFragment!!.activity
    } else {
        innerActivity
    }

    override fun getFragment(): Fragment? = innerFragment
    override fun getFragmentManager(): FragmentManager? = getFragmentActivity()?.supportFragmentManager

    override fun getFragmentActivity(): FragmentActivity? = if (innerActivity is FragmentActivity) {
        innerActivity as FragmentActivity
    } else {
        null
    }

    override fun finish() = innerActivity?.finish() ?: Unit
    override fun startActivity(intent: Intent) = innerActivity?.startActivity(intent) ?: Unit
    override fun startActivityForResult(intent: Intent, requestCode: Int) = innerActivity?.startActivityForResult(intent, requestCode)
            ?: Unit

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) =
            innerActivity?.startActivityForResult(intent, requestCode, options) ?: Unit

    // refresh

    override fun refresh() = refreshManager()
    override fun refreshGroup() = children.forEach { it.refresh() }
    override fun refreshManager() = children.forEach { it.refresh() }

    // lifecycle

    private fun putDataIfExists(key: String, value: Any?) {
        val data = swb.getData(key)
        if (data != null && data != value) {
            this.swb.putData(key, value)
        }
    }

    override fun onAttach(context: Context) {
        this.context = context
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_ATTACH)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onAttach(context) }
    }

    override fun onCreate(bundle: Bundle?) {
        this.bundle = bundle
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_CREATE)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onCreate(bundle) }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_CREATE)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onCreate(bundle) }
    }

    override fun onRestart() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_RESTART)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onRestart() }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View? {
        this.inflater = inflater
        this.bundle = bundle
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_CREATE_VIEW)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onCreateView(inflater, parent, bundle) }
        return this.view
    }

    override fun onActivityCreated(bundle: Bundle?) {
        this.bundle = bundle
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_ACTIVITY_CREATE)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onActivityCreated(bundle) }
    }

    override fun onStart() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_START)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onStart() }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_START)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onStart() }
    }

    override fun onResume() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_RESUME)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onResume() }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_RESUME)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onResume() }
    }

    override fun onPause() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_PAUSE)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onPause() }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_PAUSE)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onPause() }
    }

    override fun onStop() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_STOP)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onStop() }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_STOP)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onStop() }
    }

    override fun onDestroyView() {
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_DESTROY_VIEW)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDestroyView() }
    }

    override fun onDestroy() {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_DESTROY)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onDestroy() }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_DESTROY)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDestroy() }
    }

    override fun onDetach() {
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_DETACH)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDetach() }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_SAVE_INSTANCE_STATE)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onSaveInstanceState(bundle) }
        putDataIfExists(KEY_FRAGMENT_STATE, FragmentLifeCycleInter.STATE_SAVE_INSTANCE_STATE)
        children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onSaveInstanceState(bundle) }
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        this.bundle = bundle
        putDataIfExists(KEY_ACTIVITY_STATE, ActivityLifeCycleInter.STATE_RESTORE_INSTANCE_STATE)
        children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onRestoreInstanceState(bundle) }
    }
}

open class BlockActivity : AppCompatActivity() {
    protected open val useSameRes = true
    protected open val blockManagers = mutableListOf<BlockManager>()

    protected open fun getBlockManagerList(): List<BlockManager>? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        getBlockManagerList()?.let { blockManagers.addAll(it) }
        blockManagers.forEachIndexed { index, it ->
            if (useSameRes) {
                if (index == 0) {
                    it.initInActivity(this)
                } else {
                    it.initInBlockManager(blockManagers[0])
                }
            } else {
                it.initInActivity(this)
            }
            it.build(null)
            it.onCreate(bundle)
        }
    }

    override fun onRestart() {
        super.onRestart()
        blockManagers.forEach { it.onRestart() }
    }

    override fun onStart() {
        super.onStart()
        blockManagers.forEach { it.onStart() }
    }

    override fun onResume() {
        super.onResume()
        blockManagers.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        blockManagers.forEach { it.onPause() }
    }

    override fun onStop() {
        super.onStop()
        blockManagers.forEach { it.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        blockManagers.forEach { it.onDestroy() }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        blockManagers.forEach { it.onSaveInstanceState(bundle) }
    }

    override fun onRestoreInstanceState(bundle: Bundle) = blockManagers.forEach { it.onRestoreInstanceState(bundle) }
}

open class BlockFragment : Fragment() {
    protected open val useSameRes = true
    protected open val blockManagers = mutableListOf<BlockManager>()

    protected open fun getBlockManagerList(): List<BlockManager>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getBlockManagerList()?.let { blockManagers.addAll(it) }
        blockManagers.forEachIndexed { index, it ->
            if (useSameRes) {
                if (index == 0) {
                    it.initInFragment(this)
                } else {
                    it.initInBlockManager(blockManagers[0])
                }
            } else {
                it.initInFragment(this)
            }
            it.build(null)
            it.onAttach(context)
        }
    }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        blockManagers.forEach { it.onCreate(bundle) }
    }

    open fun onCreateView2(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        blockManagers.forEach { it.onCreateView(inflater, container, savedInstanceState) }
        return onCreateView2(inflater, container, savedInstanceState)
    }

    override fun onActivityCreated(bundle: Bundle?) {
        super.onActivityCreated(bundle)
        blockManagers.forEach { it.onActivityCreated(bundle) }
    }

    override fun onStart() {
        super.onStart()
        blockManagers.forEach { it.onStart() }
    }

    override fun onResume() {
        super.onResume()
        blockManagers.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        blockManagers.forEach { it.onPause() }
    }

    override fun onStop() {
        super.onStop()
        blockManagers.forEach { it.onStop() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        blockManagers.forEach { it.onDestroyView() }
    }

    override fun onDestroy() {
        super.onDestroy()
        blockManagers.forEach { it.onDestroy() }
    }

    override fun onDetach() {
        super.onDetach()
        blockManagers.forEach { it.onDetach() }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        blockManagers.forEach { it.onSaveInstanceState(bundle) }
    }
}

// block -> fragment
// blockGroup -> fragmentManager
// blockManager -> activity / fragment
// TODO: FragmentBlockManager
// TODO: ActivityBlockManager
