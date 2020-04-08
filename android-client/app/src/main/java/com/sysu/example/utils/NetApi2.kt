package com.sysu.example.utils

import com.liang.example.net.DEFAULT_RES_CODE
import com.liang.example.net.HttpResult
import com.liang.example.net.doGetSync
import com.liang.example.net.doPostSync

fun doGetMainAsync(
    url: String, headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit
) = Thread {
    val res = doGetSync(url, headers, targetResCode)
    ContextApi.handler.post { callback(res) }
}.start()

fun doPostMainAsync(
    url: String,
    headers: Map<String, String>? = null,
    params: ByteArray? = null,
    targetResCode: List<Int> = DEFAULT_RES_CODE,
    callback: (res: HttpResult?) -> Unit
) = Thread {
    val res = doPostSync(url, headers, params, targetResCode)
    ContextApi.handler.post { callback(res) }
}.start()
