package com.sysu.example.block

import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap

interface Consumer<T> {
    fun accept(t: T)
}

open class RxHandler : Handler() {
    companion object {
        const val TYPE_IMMEDIATE = 0
        const val TYPE_NEW_THREAD = 1
        const val TYPE_IO_THREAD = 2
        const val TYPE_COMPUTATION_THREAD = 3
        const val TYPE_SINGLE_THREAD = 4
        const val TYPE_TRAMPOLINE_THREAD = 5
        const val TYPE_MAIN_THREAD = 6
        const val TYPE_ASYNC_TASK_POOL = 7
        const val TYPE_CUSTOM = 8  // TODO
    }

    open val handlerMap: MutableMap<Int, Consumer<Message>> = ConcurrentHashMap()
    open val handlersMap: MutableMap<Any, MutableMap<Int, Consumer<Message>>> = ConcurrentHashMap()
    open val cd: CompositeDisposable = CompositeDisposable()
    open val cds: MutableMap<Any, CompositeDisposable> = ConcurrentHashMap()

    // send / post message

    open fun sendEmptyMessage(token: Any?, what: Int, delayMillis: Long = 0L, type: Int = TYPE_IMMEDIATE) {
        sendMessageDelayed(Message.obtain().apply {
            this.what = what
            this.arg1 = type
            this.obj = token
        }, delayMillis)
    }

    open fun sendMessage(token: Any?, msg: Message, delayMillis: Long = 0L, type: Int = TYPE_IMMEDIATE) {
        sendMessageDelayed(msg.apply {
            this.arg1 = type
            this.obj = token
        }, delayMillis)
    }

    open fun post(token: Any?, r: Runnable, delayMillis: Long = 0L, type: Int = TYPE_IMMEDIATE) {
        sendMessageDelayed(Message.obtain(this, r).apply {
            this.arg1 = type
            this.obj = token
        }, delayMillis)
    }

    // dispatch and handle message

    override fun dispatchMessage(msg: Message) {
        if (msg.callback != null) {
            if (msg.arg1 == TYPE_IMMEDIATE) {
                msg.callback.run()
            } else {
                cd.add(when (msg.arg1) {
                    TYPE_MAIN_THREAD -> AndroidSchedulers.mainThread()
                    TYPE_NEW_THREAD -> Schedulers.newThread()
                    TYPE_IO_THREAD -> Schedulers.io()
                    TYPE_COMPUTATION_THREAD -> Schedulers.computation()
                    TYPE_SINGLE_THREAD -> Schedulers.single()
                    TYPE_TRAMPOLINE_THREAD -> Schedulers.trampoline()
                    else -> Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)
                }.scheduleDirect(msg.callback))
            }
            return
        }
        super.dispatchMessage(msg)
    }

    override fun handleMessage(msg: Message) {
        if (handlerMap.containsKey(msg.what)) {
            if (msg.arg1 == TYPE_IMMEDIATE) {
                if (msg.obj == null) {
                    handlerMap[msg.what]!!.accept(msg)
                } else if (handlersMap.containsKey(msg.obj)) {
                    handlersMap[msg.obj]!![msg.what]!!.accept(msg)
                }
            } else {
                cd.add(when (msg.arg1) {
                    TYPE_MAIN_THREAD -> AndroidSchedulers.mainThread()
                    TYPE_NEW_THREAD -> Schedulers.newThread()
                    TYPE_IO_THREAD -> Schedulers.io()
                    TYPE_COMPUTATION_THREAD -> Schedulers.computation()
                    TYPE_SINGLE_THREAD -> Schedulers.single()
                    TYPE_TRAMPOLINE_THREAD -> Schedulers.trampoline()
                    TYPE_ASYNC_TASK_POOL -> Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)
                    else -> throw RuntimeException("arg1 is invalid")
                }.scheduleDirect {
                    if (msg.obj == null) {
                        handlerMap[msg.what]!!.accept(msg)
                    } else if (handlersMap.containsKey(msg.obj)) {
                        handlersMap[msg.obj]!![msg.what]!!.accept(msg)
                    }
                })
            }
            return
        }
        super.handleMessage(msg)
    }

    open fun dealConsumer(what: Int, consumer: Consumer<Message>? = null): Consumer<Message>? {
        return if (consumer == null) {
            handlerMap.remove(what)
        } else {
            handlerMap.put(what, consumer)
        }
    }

    open fun dealConsumerWithToken(what: Int, token: Any, consumer: Consumer<Message>? = null): Consumer<Message>? {
        if (handlersMap[token] == null) {
            handlersMap[token] = ConcurrentHashMap()
        }
        return if (consumer == null) {
            handlersMap[token]!!.remove(what)
        } else {
            handlersMap[token]!!.put(what, consumer)
        }
    }

    // disposable

    open fun register(disposable: Disposable, token: Any? = null) {
        if (token == null) {
            cd.add(disposable)
            return
        }
        if (!cds.containsKey(token)) {
            cds[token] = CompositeDisposable()
        }
        cds[token]!!.add(disposable)
    }

    open fun registerAll(token: Any?, vararg disposables: Disposable) {
        if (token == null) {
            cd.addAll(*disposables)
            return
        }
        if (!cds.containsKey(token)) {
            cds[token] = CompositeDisposable()
        }
        cds[token]!!.addAll(*disposables)
    }

    // release

    open fun release(token: Any) {
        removeCallbacksAndMessages(token)
        handlersMap.remove(token)
        if (cds.containsKey(token)) {
            cds[token]!!.dispose()
            cds.remove(token)
        }
    }

    open fun releaseAll() {
        removeCallbacksAndMessages(null)
        this.handlerMap.clear()
        this.handlersMap.clear()
        this.cd.dispose()
        this.cds.forEach { it.value.dispose() }
        this.cds.clear()
    }
}
