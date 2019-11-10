package com.sysu.deepnavi.util

import android.util.Log
import com.sysu.deepnavi.inter.LoggerInter
import com.sysu.deepnavi.inter.LoggerInter.LoggerLevel

fun doNotNeedImpl() {}

var DEFAULT_LEVEL: LoggerLevel = LoggerLevel.VERBOSE
var DEFAULT_TAG: String = "DeepNavi"
val EMPTY_ARRAY = emptyArray<Any>()

class AndroidLogLogger(override var logLevel: LoggerLevel = DEFAULT_LEVEL) : LoggerInter {
    private inline fun log(
        tag: String, msg: String, error: Throwable?, method1: (String, String) -> Int,
        method2: (String, String, Throwable) -> Int, level: LoggerLevel, vararg args: Any?
    ): Int =
        when {
            logLevel > level -> 0
            error == null -> method1.invoke(tag, if (args.isNotEmpty()) String.format(msg, *args) else msg)
            else -> method2.invoke(tag, if (args.isNotEmpty()) String.format(msg, *args) else msg, error)
        }

    override fun v(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.v(t2, m2) }, { t2, m2, t3 -> Log.v(t2, m2, t3) }, LoggerLevel.VERBOSE, *args)

    override fun d(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.d(t2, m2) }, { t2, m2, t3 -> Log.d(t2, m2, t3) }, LoggerLevel.DEBUG, *args)

    override fun i(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.i(t2, m2) }, { t2, m2, t3 -> Log.i(t2, m2, t3) }, LoggerLevel.INFO, *args)

    override fun w(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.w(t2, m2) }, { t2, m2, t3 -> Log.w(t2, m2, t3) }, LoggerLevel.WARN, *args)

    override fun e(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.e(t2, m2) }, { t2, m2, t3 -> Log.e(t2, m2, t3) }, LoggerLevel.ERROR, *args)

    override fun wtf(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int =
        log(tag.toString(), msg, error, { t2, m2 -> Log.wtf(t2, m2) }, { t2, m2, t3 -> Log.wtf(t2, m2, t3) }, LoggerLevel.FATAL, *args)
}
