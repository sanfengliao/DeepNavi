package com.sysu.example.utils

import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

val DEFAULT_RES_CODE = listOf(HttpURLConnection.HTTP_OK)

data class HttpResult(val con: HttpURLConnection, val content: ByteArray?, val resCode: Int, val e: Exception?) {
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

private fun httpSync(url: String, headers: Map<String, String>?, targetResCode: List<Int>, prepare: ((con: HttpURLConnection) -> Unit)? = null): HttpResult? {
    val con = URL(url).openConnection() as HttpURLConnection
    con.doInput = true
    con.connectTimeout = 3000
    con.readTimeout = 3000
    headers?.forEach { con.addRequestProperty(it.key, it.value) }
    prepare?.invoke(con)
    try {
        con.connect()
    } catch (e: Exception) {
        return HttpResult(con, null, -1, e)
    }
    val resCode = con.responseCode
    return try {
        if (resCode in targetResCode && con.inputStream != null) {
            HttpResult(con, con.inputStream.readBytes(), resCode, null)
        } else if (con.errorStream != null) {
            HttpResult(con, con.errorStream.readBytes(), resCode, null)
        } else {
            HttpResult(con, null, resCode, null)
        }
    } catch (e: Exception) {
        HttpResult(con, null, resCode, e)
    } finally {
        con.inputStream?.close()
        con.errorStream?.close()
    }
}

fun doGetSync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
    httpSync(url, headers, targetResCode)

fun doPostSync(url: String, headers: Map<String, String>?, params: ByteArray?, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
    httpSync(url, headers, targetResCode) { con ->
        con.doOutput = true
        con.requestMethod = "POST"
        if (params != null) {
            val output = con.outputStream
            if (output != null) {
                output.write(params)
                output.flush()
                output.close()
            }
            con.outputStream?.close()
        }
    }

fun doGetAsync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit) =
    Thread { callback(doGetSync(url, headers, targetResCode)) }.start()

fun doPostAsync(url: String, headers: Map<String, String>?, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit) =
    Thread { callback(doGetSync(url, headers, targetResCode)) }.start()
