@file:Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")

package com.sysu.example.block

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.*
import io.reactivex.Observable
import io.reactivex.annotations.Nullable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.lang.RuntimeException
import java.util.concurrent.ConcurrentHashMap

open class WhiteBoard<T> : ViewModel() {
    private val dataMap = ConcurrentHashMap<T, Any>()
    private val subjectMap = ConcurrentHashMap<T, Subject<Any>>()

    // liveData

    fun putLiveData(key: T, value: Any?) = getLiveData(key).postValue(value)

    fun removeLiveData(key: T) = dataMap.remove(key) as? StrongLiveData<Any?>

    fun getLiveData(key: T): StrongLiveData<Any?> {
        var liveData = dataMap[key]
        if (liveData != null && liveData !is StrongLiveData<*>) {
            throw RuntimeException("put wrong value for a data which isn't a livedata")
        }
        if (liveData == null) {
            liveData = StrongLiveData<Any?>()
        }
        return liveData as StrongLiveData<Any?>
    }

    // rx

    fun notifyDataChanged(key: T) = subjectMap[key]?.onNext(dataMap[key] ?: NULL_OBJECT)

    fun putData(key: T, value: Any?) {
        putDataWithoutNotify(key, value)
        notifyDataChanged(key)
    }

    fun putDataWithoutNotify(key: T, value: Any?) {
        if (value == null) {
            dataMap.remove(key)
        } else {
            dataMap[key] = value
        }
    }

    fun removeData(key: T) {
        dataMap.remove(key)
        notifyDataChanged(key)
    }

    @Nullable
    fun getData(key: T) = dataMap[key]

    @Suppress("UNCHECKED_CAST")
    fun <T2 : Any?> getObservable(key: T, threadSafe: Boolean): Observable<T2> {
        if (!subjectMap.containsKey(key)) {
            subjectMap[key] = PublishSubject.create<Any>()
            if (threadSafe) {
                subjectMap[key] = subjectMap[key]!!.toSerialized()
            }
        }
        val res: Subject<Any> = subjectMap[key]!!
        return if (dataMap[key] != null) {
            res.startWith(dataMap[key])
        } else {
            res
        } as Observable<T2>
    }

    companion object {
        val NULL_OBJECT = Any()

        fun <T> create() = WhiteBoard<T>()

        fun <T> of(key: String, provider: StrongViewModelProvider): WhiteBoard<T> {
            var result = provider.get<WhiteBoard<T>>(key)
            if (result == null) {
                result = WhiteBoard()
                provider[key] = result
            }
            return result
        }

        inline fun <reified T> of(provider: StrongViewModelProvider): WhiteBoard<T> = of("WhiteBoard${T::class.java.canonicalName}", provider)

        inline fun <reified T> of(storeOwner: ViewModelStoreOwner): WhiteBoard<T> =
                // return ViewModelProvider(storeOwner).get("WhiteBoard${T::class.java.canonicalName}", WhiteBoard::class.java) as WhiteBoard<T>
                of(StrongViewModelProvider(storeOwner))

        inline fun <reified T> of(storeOwner: ViewModelStoreOwner, factory: ViewModelProvider.Factory): WhiteBoard<T> =
                // ViewModelProvider(storeOwner, factory).get("WhiteBoard${T::class.java.canonicalName}", WhiteBoard::class.java) as WhiteBoard<T>
                of(StrongViewModelProvider(storeOwner, factory))

        inline fun <reified T> of(store: ViewModelStore, factory: ViewModelProvider.Factory): WhiteBoard<T> =
                // ViewModelProvider(store, factory).get("WhiteBoard${T::class.java.canonicalName}", WhiteBoard::class.java) as WhiteBoard<T>
                of(StrongViewModelProvider(store, factory))
    }
}

fun WhiteBoard<String>.putBundle(arguments: Bundle?) {
    return if (arguments != null) arguments.keySet().forEach { putData(it, arguments.get(it)) } else Unit
}

fun WhiteBoard<String>.putIntent(intent: Intent?): Unit? {
    return if (intent != null) {
        val extras = intent.extras
        if (extras != null) {
            extras.keySet().forEach { putData(it, extras.get(it)) }
        } else Unit
    } else Unit
}

fun <T : Any> WhiteBoard<Class<*>>.putLiveData(value: T) = putLiveData(value::class.java, value)
fun <T : Any> WhiteBoard<Class<*>>.putData(value: T) = putData(value::class.java, value)
fun <T : Any> WhiteBoard<Class<*>>.putDataWithoutNotify(value: T) = putDataWithoutNotify(value::class.java, value)
