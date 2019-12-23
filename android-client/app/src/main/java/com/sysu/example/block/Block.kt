@file: Suppress("UNCHECKED_CAST", "LeakingThis")

package com.sysu.example.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.lifecycle.ViewModelStoreOwner
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MemberVisibilityCanBePrivate")
open class Block() : ActivityProxy() {
    companion object {
        const val KEY_INFLATED = "key_inflated"
    }

    protected open var provider: StrongViewModelProvider? = null
    protected open lateinit var swb: WhiteBoard<String>
    protected open lateinit var cwb: WhiteBoard<Class<*>>
    protected open lateinit var rxHandler: RxHandler

    protected open var blockGroup: BlockGroup? = null
    protected open var blockManager: BlockManager? = null

    // init

    constructor(@LayoutRes layoutId: Int) : this() {
        this.layoutId = layoutId
    }

    constructor(context: Context, @LayoutRes layoutId: Int = 0) : this(layoutId) {
        init(context)
    }

    // 可以使用这个而不依靠BlockManager/BlockGroup
    open fun init(context: Context): Block {
        this.context = context
        this.inflater = LayoutInflater.from(this.context)
        if (context is ViewModelStoreOwner) {
            this.provider = StrongViewModelProvider(context)
            this.swb = WhiteBoard.of(this.provider!!)
            this.cwb = WhiteBoard.of(this.provider!!)
        } else {
            this.swb = WhiteBoard.create()
            this.cwb = WhiteBoard.create()
        }
        this.rxHandler = RxHandler()
        return this
    }

    open fun init(blockGroup: BlockGroup): Block {
        this.context = blockGroup.context
        this.inflater = blockGroup.inflater
        this.provider = blockGroup.provider
        this.swb = blockGroup.swb
        this.cwb = blockGroup.cwb
        this.rxHandler = blockGroup.rxHandler
        return this
    }

    open fun init(blockManager: BlockManager): Block {
        this.context = blockManager.context
        this.inflater = blockManager.inflater
        this.provider = blockManager.provider
        this.swb = blockManager.swb
        this.cwb = blockManager.cwb
        this.rxHandler = blockManager.rxHandler
        return this
    }

    open fun init(swb: WhiteBoard<String>, cwb: WhiteBoard<Class<*>>, h: RxHandler, bg: BlockGroup? = null, bm: BlockManager? = null): Block {
        this.swb = swb
        this.cwb = cwb
        this.rxHandler = h
        this.blockGroup = bg
        if (bg != null) {
            this.parent = bg.viewGroup
        }
        this.blockManager = bm
        if (bm != null) {
            this.provider = bm.provider
        } else if (bg != null) {
            this.provider = bg.provider
        }
        this.ai = bm
        this.swb.putData(KEY_INFLATED, false)
        return this
    }

    // inflate

    open lateinit var context: Context
    open lateinit var inflater: LayoutInflater
    open var parent: ViewGroup? = null
    open var view: View? = null
    open val viewId: Int
        get() = if (inflated.get() && view != null) view!!.id else View.NO_ID
    open var inflated = AtomicBoolean(false)

    @LayoutRes
    open var layoutId: Int = 0
    open var inflateViewAsync: Boolean = false
    open val inflateViewDelay: Long = 0L

    protected var afterInflateListener: Runnable? = null
    open fun beforeInflateView() = Unit
    open fun afterInflateView() = Unit
    open fun onInflateView(context: Context, inflater: LayoutInflater, parent: ViewGroup?): View? = inflater.inflate(layoutId, null, false)

    open fun <T : View> setInflatedCallback(callback: (T) -> Unit): Block {
        afterInflateListener = Runnable { callback(view as T) }
        return this
    }

    open fun inflate(context: Context, inflater: LayoutInflater, parent: ViewGroup?): Block {
        if (inflated.get()) {
            return this
        }
        this.context = context
        this.inflater = inflater
        this.parent = parent
        val inflateTask = Runnable {
            beforeInflateView()
            view = onInflateView(context, inflater, parent)
                    ?: inflater.inflate(layoutId, null, false)
            Log.d("Block", "inflate -- view: ${view!!.javaClass.name}, viewId: $viewId, ${parent?.javaClass?.name}, ${this.javaClass.name}")
            if (view!!.parent == null) {
                val task = when {
                    blockGroup != null -> Runnable { blockGroup!!.addViewOfBlock(this) }
                    parent != null && index == -1 -> Runnable { parent.addView(view) }
                    parent != null && index != -1 -> Runnable { parent.addView(view, index) }  // TODO: 真正安全
                    else -> Runnable { }
                }
                if (inflateViewAsync) {
                    post(task, type = RxHandler.TYPE_MAIN_THREAD)
                } else {
                    task.run()
                }
            }
            inflated.compareAndSet(false, true)
            putData(KEY_INFLATED, true)
            afterInflateListener?.run()
            afterInflateView()
        }
        if (inflateViewAsync) {
            post(inflateTask, inflateViewDelay, RxHandler.TYPE_NEW_THREAD)
        } else {
            inflateTask.run()
        }
        return this
    }

    protected var index = -1
    open fun build(parent: ViewGroup? = null, index: Int? = null): Block {
        this.index = index ?: -1
        inflate(this.context, this.inflater, parent ?: this.parent)
        return this
    }

    // recycle

    open fun recycle() {  // TODO: 验证
        this.rxHandler.release(this)
        if (this.view != null && this.view!!.parent != null) {
            (this.view!!.parent as ViewGroup).removeView(this.view)
        }
        this.parent = null
    }

    // refresh

    open fun refresh() = Unit
    open fun refreshGroup(): Unit = blockGroup?.refreshGroup() ?: Unit
    open fun refreshManager(): Unit = blockManager?.refreshManager() ?: Unit

    // observable / data

    open fun putData(key: String, value: Any?) = swb.putData(key, value)
    open fun putDataWithoutNotify(key: String, value: Any?) = swb.putDataWithoutNotify(key, value)
    open fun removeData(key: String) = swb.removeData(key)
    open fun getData(key: String) = swb.getData(key)
    open fun getObservable(key: String, threadSafe: Boolean = false): Observable<Any?> = swb.getObservable(key, threadSafe)

    open fun putBundle(bundle: Bundle?) = swb.putBundle(bundle)
    open fun putIntent(intent: Intent?) = swb.putIntent(intent)

    open fun putData(key: Class<*>, value: Any?) = cwb.putData(key, value)
    open fun putDataWithoutNotify(key: Class<*>, value: Any?) = cwb.putDataWithoutNotify(key, value)
    open fun removeData(key: Class<*>) = cwb.removeData(key)
    open fun getData(key: Class<*>) = cwb.getData(key)
    open fun getObservable(key: Class<*>, threadSafe: Boolean = false): Observable<Any?> = cwb.getObservable(key, threadSafe)

    open fun putData(value: Any) = cwb.putData(value)
    open fun putDataWithoutNotify(value: Any) = cwb.putDataWithoutNotify(value)

    open fun putLiveData(key: String, value: Any?) = swb.putLiveData(key, value)
    open fun removeLiveData(key: String) = swb.removeLiveData(key)
    open fun getLiveData(key: String) = swb.getLiveData(key)

    open fun putLiveData(value: Any) = cwb.putLiveData(value)
    open fun putLiveData(key: Class<*>, value: Any?) = cwb.putLiveData(key, value)
    open fun removeLiveData(key: Class<*>) = cwb.removeLiveData(key)
    open fun getLiveData(key: Class<*>) = cwb.getLiveData(key)

    // handler / disposable

    open fun dealConsumer(what: Int, consumer: Consumer<Message>? = null) = this.rxHandler.dealConsumer(what, consumer)
    open fun sendEmptyMessage(what: Int, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) =
            rxHandler.sendEmptyMessage(null, what, delayMillis, type)

    open fun sendMessage(msg: Message, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) =
            rxHandler.sendMessage(null, msg, delayMillis, type)

    open fun post(r: Runnable, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) = rxHandler.post(null, r, delayMillis, type)

    open fun dealConsumerInner(what: Int, consumer: Consumer<Message>? = null) = this.rxHandler.dealConsumerWithToken(what, this, consumer)
    open fun sendEmptyMessageInner(what: Int, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) =
            rxHandler.sendEmptyMessage(this, what, delayMillis, type)

    open fun sendMessageInner(msg: Message, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) =
            rxHandler.sendMessage(this, msg, delayMillis, type)

    open fun postInner(r: Runnable, delayMillis: Long = 0L, type: Int = RxHandler.TYPE_IMMEDIATE) = rxHandler.post(this, r, delayMillis, type)

    open fun register(disposable: Disposable, inBlock: Boolean = false) = rxHandler.register(disposable, if (inBlock) this else null)
    open fun registerAll(inBlock: Boolean, vararg disposables: Disposable) = rxHandler.registerAll(if (inBlock) this else null, *disposables)
}

open class FragmentBlock : Block(), FragmentLifeCycleInter {
    open var bundle: Bundle? = null

    // lifecycle

    @CallSuper
    override fun onAttach(context: Context) {
        this.context = context
    }

    @CallSuper
    override fun onCreate(bundle: Bundle?) {
        this.bundle = bundle
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View? {
        this.bundle = bundle
        return this.view
    }

    @CallSuper
    override fun onActivityCreated(bundle: Bundle?) {
        this.bundle = bundle
    }

    override fun onStart() = Unit
    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun onStop() = Unit
    override fun onDestroyView() = Unit

    @CallSuper
    override fun onDestroy() = this.rxHandler.release(this)

    override fun onDetach() = Unit

    override fun onSaveInstanceState(bundle: Bundle) = Unit
}

open class ActivityBlock : Block(), ActivityLifeCycleInter {
    open var bundle: Bundle? = null

    @CallSuper
    override fun onCreate(bundle: Bundle?) {
        this.bundle = bundle
    }

    override fun onRestart() = Unit
    override fun onStart() = Unit
    override fun onResume() = Unit
    override fun onPause() = Unit
    override fun onStop() = Unit

    @CallSuper
    override fun onDestroy() = this.rxHandler.release(this)

    override fun onSaveInstanceState(bundle: Bundle) = Unit

    @CallSuper
    override fun onRestoreInstanceState(bundle: Bundle) {
        this.bundle = bundle
    }
}
