package com.sysu.example

import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import kotlin.collections.ArrayList

open class Headers : Cloneable {
    private val namesAndValues: ArrayList<String> = ArrayList()

    constructor(vararg namesAndValues: String) {
        val size = namesAndValues.size
        if (size.and(1) != 0) {
            throw IllegalArgumentException("Expected alternating header names and values")
        }
        this.namesAndValues.ensureCapacity(size)
        (0 until size.shr(1)).forEach { plus(namesAndValues[it], namesAndValues[it + 1]) }
    }

    constructor(namesAndValues: List<String>) {
        val size = namesAndValues.size
        if (size.and(1) != 0) {
            throw IllegalArgumentException("Expected alternating header names and values")
        }
        this.namesAndValues.ensureCapacity(size)
        (0 until size.shr(1)).forEach { plus(namesAndValues[it], namesAndValues[it + 1]) }
    }

    constructor(headers: Map<String, String>) {
        this.namesAndValues.ensureCapacity(headers.size.shl(1))
        headers.forEach { plus(it.key, it.value) }
    }

    open fun size() = namesAndValues.size / 2
    open fun name(index: Int) = namesAndValues[index * 2]
    open fun value(index: Int) = namesAndValues[index * 2 + 1]

    operator fun plus(entry: Map.Entry<String, String>) = plus(entry.key, entry.value)
    operator fun plus(pair: Pair<String, String>) = plus(pair.first, pair.second)
    operator fun plus(line: String) {
        val separatorIndex = line.indexOf(":")
        if (separatorIndex == -1) {
            throw java.lang.IllegalArgumentException("Unexpected header: $line")
        }
        plus(line.substring(0, separatorIndex), line.substring(separatorIndex + 1))
    }

    fun plus(n: String, v: String) {
        val name = n.trim()
        val value = v.trim()
        if (name.isEmpty() || value.isEmpty()) {
            throw IllegalArgumentException("Unexpected header: $name: $value")
        }
        this.namesAndValues.add(name)
        this.namesAndValues.add(value)
    }

    operator fun minus(name: String) {
        (size() - 1 downTo 0).forEach {
            if (name(it).contentEquals(name)) {
                val rightIndex = it.shl(1)
                this.namesAndValues.removeAt(it * 2)
                this.namesAndValues.removeAt(it * 2 + 1)
            }
        }
    }

    operator fun set(name: String, value: String) {
        minus(name)
        plus(name, value)
    }

    operator fun get(name: String): String? {
        var i = namesAndValues.size - 2
        while (i >= 0) {
            if (name.equals(namesAndValues[i], ignoreCase = true)) {
                return namesAndValues[i + 1]
            }
            i -= 2
        }
        return null
    }

    open fun names(): Set<String> {
        val result: TreeSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        (0 until size()).forEach { result.add(name(it)) }
        return Collections.unmodifiableSet(result)
    }

    open fun values(name: String): List<String> {
        var result: MutableList<String>? = null
        (0 until size()).forEach {
            if (name.equals(name(it), ignoreCase = true)) {
                if (result == null) result = ArrayList(2)
                result!!.add(value(it))
            }
        }
        return if (result != null) Collections.unmodifiableList(result!!) else emptyList()
    }

    override fun equals(other: Any?): Boolean = other is Headers && namesAndValues.toTypedArray().contentEquals(other.namesAndValues.toTypedArray())
    override fun hashCode(): Int = namesAndValues.toTypedArray().contentHashCode()
    override fun toString(): String {
        val result = StringBuilder()
        (0 until size()).forEach { result.append(name(it)).append(": ").append(value(it)).append("\n") }
        return result.toString()
    }

    open fun toMultimap(): Map<String, MutableList<String>> {
        val result: MutableMap<String, MutableList<String>> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)
        (0 until size()).forEach {
            val name = name(it).toLowerCase(Locale.US)
            var values = result[name]
            if (values == null) {
                values = ArrayList(2)
                result[name] = values
            }
            values.add(value(it))
        }
        return result
    }

    override fun clone(): Headers = Headers(namesAndValues)
}

open class Request<T : Any> {
    enum class State {
        CREATED, WAITING, REQUESTING, RESPONDED
    }

    var shouldCache: NetPolicy.CachePolicy = NetPolicy.CachePolicy.INHERIT
    var tag: Any? = null
    var priority: Int = 2
    var state: State = State.CREATED

    var url: String? = null
    var method: String? = null
    var extra: T? = null
    var headers: Headers? = null
}

open class Response<T : Any> {
    var date: Long = -1L
    var fromCache: Boolean = true

    var data: ByteArray? = null
    var result: T? = null
    var headers: Headers? = null

    interface ResponseListener {
        fun onSuccess(response: Response<*>)
        fun onFail(ex: Exception)
    }
}

interface Cache {
    data class Entry(
        val key: String,
        var response: Response<*>,
        val serverDate: Long,
        var lastModified: Long,
        var ttl: Long = 20 * 60 * 1000  // 20 minutes
    )

    fun put(key: String, response: Response<*>)
    fun remove(key: String)
    operator fun get(key: String): Entry
    fun clear()
    fun initialize()
}

interface NetPolicy {
    enum class CachePolicy {
        NO, YES, INHERIT
    }

    enum class DispatcherPolicy {
        // TODO: rxjava 里面的 schedulers 的那些
    }

    fun retry(requestQueue: RequestQueue, request: Request<*>)
    fun maxRetryCount(): Int = 1
    fun currentRetryTime(): Long = 5000
    fun shouldCache(): CachePolicy = CachePolicy.YES
    fun maxPriority(): Int = 4
    fun threadPoolNum(): Int = 4
}

interface NetWork {
    fun execute(outputStream: OutputStream): InputStream
    // TODO: status_code 等等
}

open class NetWorkWorker : Runnable {
    override fun run() {
    }
}

open class CacheWorker : Runnable {
    override fun run() {
    }
}

open class RequestQueue(val policy: NetPolicy, val cache: Cache, val netWork: NetWork) {
    companion object {
    }

    val priorityRequests: List<PriorityBlockingQueue<Request<*>>> = (0 until policy.maxPriority()).map { PriorityBlockingQueue<Request<*>>() }

    fun addRequest(request: Request<*>) {
        // TODO: 校验
        if (request.shouldCache == NetPolicy.CachePolicy.INHERIT) {
            request.shouldCache = policy.shouldCache()
        }
    }
}

interface RequestHandler {
    fun parseFromRequest(request: Request<*>): OutputStream
}

interface ResponseHandler {
    fun parseToResponse(inputStream: InputStream): Response<*>
}
