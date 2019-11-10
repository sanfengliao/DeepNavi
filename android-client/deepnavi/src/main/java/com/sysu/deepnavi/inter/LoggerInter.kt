package com.sysu.deepnavi.inter

import com.sysu.deepnavi.util.EMPTY_ARRAY

interface LoggerInter {
    enum class LoggerLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL, NONE;
    }

    var logLevel: LoggerLevel
    fun isLoggable(level: LoggerLevel): Boolean = level >= logLevel

    fun v(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.VERBOSE) v(tag, msg, null, args = *EMPTY_ARRAY) else 0
    fun d(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.DEBUG) d(tag, msg, null, args = *EMPTY_ARRAY) else 0
    fun i(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.INFO) i(tag, msg, null, args = *EMPTY_ARRAY) else 0
    fun w(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.WARN) w(tag, msg, null, args = *EMPTY_ARRAY) else 0
    fun e(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.ERROR) e(tag, msg, null, args = *EMPTY_ARRAY) else 0
    fun wtf(tag: Any, msg: String): Int = if (logLevel <= LoggerLevel.FATAL) wtf(tag, msg, null, args = *EMPTY_ARRAY) else 0

    fun v(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.VERBOSE) v(tag, msg, null, args = *args) else 0
    fun d(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.DEBUG) d(tag, msg, null, args = *args) else 0
    fun i(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.INFO) i(tag, msg, null, args = *args) else 0
    fun w(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.WARN) w(tag, msg, null, args = *args) else 0
    fun e(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.ERROR) e(tag, msg, null, args = *args) else 0
    fun wtf(tag: Any, msg: String, vararg args: Any?): Int = if (logLevel <= LoggerLevel.FATAL) wtf(tag, msg, null, args = *args) else 0

    fun v(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.VERBOSE) v(tag, msg, error, args = *EMPTY_ARRAY) else 0
    fun d(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.DEBUG) d(tag, msg, error, args = *EMPTY_ARRAY) else 0
    fun i(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.INFO) i(tag, msg, error, args = *EMPTY_ARRAY) else 0
    fun w(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.WARN) w(tag, msg, error, args = *EMPTY_ARRAY) else 0
    fun e(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.ERROR) e(tag, msg, error, args = *EMPTY_ARRAY) else 0
    fun wtf(tag: Any, msg: String, error: Throwable?): Int = if (logLevel <= LoggerLevel.FATAL) wtf(tag, msg, error, args = *EMPTY_ARRAY) else 0

    fun v(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
    fun d(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
    fun i(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
    fun w(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
    fun e(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
    fun wtf(tag: Any, msg: String, error: Throwable?, vararg args: Any?): Int
}
