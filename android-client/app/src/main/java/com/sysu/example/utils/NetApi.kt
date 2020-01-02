package com.sysu.example.utils

import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

// [HttpURLConnection 使用总结](https://www.jianshu.com/p/cfefdc4e062e)
// [【封装】异步HttpURLConnection网络访问](https://blog.csdn.net/u013806583/article/details/69916214)

val DEFAULT_RES_CODE = listOf(HttpURLConnection.HTTP_OK)

data class HttpResult(val con: HttpURLConnection, val content: ByteArray?, val resCode: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as HttpResult
        if (con != other.con || content != null && (other.content == null || !content.contentEquals(other.content)) || other.content != null) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = con.hashCode()
        result = 31 * result + (content?.contentHashCode() ?: 0)
        return result
    }
}

private fun httpSync(url: String, headers: Map<String, String>?, targetResCode: List<Int>, prepare: (con: HttpURLConnection) -> Unit): HttpResult? {
    val con = URL(url).openConnection() as HttpURLConnection
    con.connectTimeout = 3000
    con.readTimeout = 3000
    headers?.forEach { con.addRequestProperty(it.key, it.value) }
    prepare(con)
    con.connect()
    val resCode = con.responseCode
    return try {
        if (resCode in targetResCode && con.inputStream != null) {
            HttpResult(con, con.inputStream.readBytes(), resCode)
        } else if (con.errorStream != null) {
            HttpResult(con, con.errorStream.readBytes(), resCode)
        } else {
            HttpResult(con, null, resCode)
        }
    } catch (e: Exception) {
        HttpResult(con, null, resCode)
    } finally {
        con.inputStream?.close()
        con.outputStream?.close()
        con.errorStream?.close()
    }
}

fun doGetSync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
    httpSync(url, headers, targetResCode) {}

fun doPostSync(url: String, headers: Map<String, String>?, params: ByteArray?, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
    httpSync(url, headers, targetResCode) { con ->
        con.requestMethod = "POST"
        if (params != null) {
            val output = con.outputStream
            if (output != null) {
                output.write(params)
                output.flush()
                output.close()
            }
        }
    }

fun doGetAsync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit) =
    Thread { callback(doGetSync(url, headers, targetResCode)) }.start()

fun doPostAsync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit) =
    Thread { callback(doGetSync(url, headers, targetResCode)) }.start()
