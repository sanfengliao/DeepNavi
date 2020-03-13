package com.sysu.example.utils

import io.reactivex.internal.fuseable.QueueFuseable.BOUNDARY
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

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

    override fun toString(): String = "con: $con, content: $content, resCode: $resCode, e: $e"
}

private fun httpSync(
    url: String,
    headers: Map<String, String>? = null,
    targetResCode: List<Int> = DEFAULT_RES_CODE,
    prepare: ((con: HttpURLConnection) -> Unit)? = null
): HttpResult? {
    val con = URL(url).openConnection() as HttpURLConnection
    con.doInput = true
    con.connectTimeout = 3000
    con.readTimeout = 3000
    headers?.forEach { con.addRequestProperty(it.key, it.value) }
    prepare?.invoke(con)
    val resCode = try {
        con.connect()
        con.responseCode
    } catch (e: Exception) {
        return HttpResult(con, null, -1, e)
    }
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

fun doGetSync(url: String, headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
    httpSync(url, headers, targetResCode)

fun doPostSync(url: String, headers: Map<String, String>? = null, params: ByteArray? = null, targetResCode: List<Int> = DEFAULT_RES_CODE): HttpResult? =
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
        }
    }

fun doGetAsync(
    url: String, headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit
) =
    Thread { callback(doGetSync(url, headers, targetResCode)) }.start()

fun doPostAsync(
    url: String,
    headers: Map<String, String>? = null,
    params: ByteArray? = null,
    targetResCode: List<Int> = DEFAULT_RES_CODE,
    callback: (res: HttpResult?) -> Unit
) =
    Thread { callback(doPostSync(url, headers, params, targetResCode)) }.start()

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

const val PREFIX = "--"
const val LINEND = "\r\n"
const val MULTIPART_FROM_DATA = "multipart/form-data"
const val CHARSET = "UTF-8"

fun <T> doPostFileSync(
    url: String, params: Map<String, String>, files: Map<String, T>,
    headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE, callable: (dos: DataOutputStream, file: T) -> Unit
): HttpResult? {
    val boundary = UUID.randomUUID().toString()
    return httpSync(url, headers, targetResCode) { con ->
        con.readTimeout = 10 * 1000 // 缓存的最长时间
        con.useCaches = false
        con.doOutput = true
        con.requestMethod = "POST"
        con.setRequestProperty("connection", "keep-alive")
        con.setRequestProperty("Charsert", "UTF-8")
        con.setRequestProperty("Content-Type", "$MULTIPART_FROM_DATA;boundary=$boundary")
        // 首先组拼文本类型的参数
        val sb = StringBuilder()
        for ((key, value) in params) {
            sb.append(PREFIX)
            sb.append(BOUNDARY)
            sb.append(LINEND)
            sb.append("Content-Disposition: form-data; name=\"$key\"$LINEND")
            sb.append("Content-Type: text/plain; charset=$CHARSET$LINEND")
            sb.append("Content-Transfer-Encoding: 8bit$LINEND")
            sb.append(LINEND)
            sb.append(value)
            sb.append(LINEND)
        }
        val outStream = DataOutputStream(con.outputStream)
        outStream.write(sb.toString().toByteArray())
        // 发送文件数据
        for ((name, file) in files) {
            val sb1 = StringBuilder()
            sb1.append(PREFIX)
            sb1.append(BOUNDARY)
            sb1.append(LINEND)
            sb1.append("Content-Disposition: form-data; name=\"uploadfile\"; filename=\"$name\"$LINEND")
            sb1.append("Content-Type: application/octet-stream; charset=$CHARSET$LINEND")
            sb1.append(LINEND)
            outStream.write(sb1.toString().toByteArray())
            callable(outStream, file)
            outStream.write(LINEND.toByteArray())
        }
        // 请求结束标志
        outStream.write((PREFIX + BOUNDARY + PREFIX + LINEND).toByteArray())
        outStream.flush()
        outStream.close()
    }
}

fun doPostFileSync2(
    url: String, params: Map<String, String>, files: Map<String, File>,
    headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE
): HttpResult? = doPostFileSync(url, params, files, headers, targetResCode) { dos, file ->
    val fis = FileInputStream(file)
    val buffer = ByteArray(1024)
    var len: Int
    while (fis.read(buffer).also { len = it } != -1) {
        dos.write(buffer, 0, len)
    }
    fis.close()
}

fun doPostAsync2(
    url: String, params: Map<String, String>, files: Map<String, File>,
    headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit
) = Thread { callback(doPostFileSync2(url, params, files, headers, targetResCode)) }.start()

fun doPostSync3(
    url: String, params: Map<String, String>, files: Map<String, ByteArray>,
    headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE
): HttpResult? = doPostFileSync(url, params, files, headers, targetResCode) { dos, file -> dos.write(file) }

fun doPostAsync3(
    url: String, params: Map<String, String>, files: Map<String, ByteArray>,
    headers: Map<String, String>? = null, targetResCode: List<Int> = DEFAULT_RES_CODE, callback: (res: HttpResult?) -> Unit
) = Thread { callback(doPostSync3(url, params, files, headers, targetResCode)) }.start()

var MEDIA_TYPE_TEXT: MediaType = "text/plain".toMediaType()
var MEDIA_TYPE_IMAGE: MediaType = "image/*".toMediaType()

fun sendMultiPart(url: String, params: Map<String, String>, files: Map<String, ByteArray>, callback: (res: ByteArray?) -> Unit) {
    val httpClient = OkHttpClient()
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
    params.forEach { (key, value) -> requestBody.addFormDataPart(key, value) }
    files.forEach { (name, bytes) -> requestBody.addFormDataPart(name, name, RequestBody.create(MEDIA_TYPE_TEXT, bytes)) }
    val request = Request.Builder()
        .url(url)
        .post(requestBody.build())
        .build()
    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = Unit
        override fun onResponse(call: Call, response: Response) = callback(response.body?.bytes())
    })
}
