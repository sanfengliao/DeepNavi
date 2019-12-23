package com.sysu.example.block

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.lifecycle.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

open class StrongLiveData<T> : MutableLiveData<T>() {
    open var useEquals = false
    protected open val myObserverListener: ObserverListener? = null

    @MainThread
    override fun setValue(@Nullable t: T?) {
        if (!useEquals || t != value) {
            super.setValue(t)
        }
    }

    // onActive / onInActive

    override fun onActive() = myObserverListener?.onActive() ?: Unit
    override fun onInactive() = myObserverListener?.onInactive() ?: Unit
    interface ObserverListener {
        fun onActive() {}
        fun onInactive() {}
    }
}

@Suppress("UNCHECKED_CAST")
open class StrongViewModelProvider(private val mViewModelStore: ViewModelStore, private val mFactory: Factory) :
        ViewModelProvider(mViewModelStore, mFactory) {
    companion object {
        private const val DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey"
        private var getMethod: Method? = null
        private var putMethod: Method? = null

        init {
            for (i in 0..2) {
                getMethod = try {
                    ViewModelStore::class.java.getDeclaredMethod("get", String::class.java)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Impossible occurred exception in MyViewModelProvider about getMethod", e)
                }
                putMethod = try {
                    ViewModelStore::class.java.getDeclaredMethod("put", String::class.java, ViewModel::class.java)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Impossible occurred exception in MyViewModelProvider about putMethod", e)
                }
            }
            getMethod!!.isAccessible = true
            putMethod!!.isAccessible = true
        }
    }

    abstract class KeyedFactory : Factory {
        /**
         * Creates a new instance of the given `Class`.
         *
         * @param key        a key associated with the requested ViewModel
         * @param modelClass a `Class` whose instance is requested
         * @param <T>        The type parameter for the ViewModel.
         * @return a newly created ViewModel
        </T> */
        abstract fun <T : ViewModel?> create(key: String,
                                             modelClass: Class<T>): T

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException("create(String, Class<?>) must be called on "
                    + "implementaions of KeyedFactory")
        }
    }

    constructor(owner: ViewModelStoreOwner, factory: Factory) : this(owner.viewModelStore, factory)
    constructor(owner: ViewModelStoreOwner) : this(owner, if (owner is HasDefaultViewModelProviderFactory) {
        (owner as HasDefaultViewModelProviderFactory).defaultViewModelProviderFactory
    } else {
        DefaultMyFactory.instance as Factory
    })

    open operator fun <T : ViewModel> get(modelClass: Class<T>, vararg args: Any): T {
        val canonicalName = modelClass.canonicalName
                ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
        return get("$DEFAULT_KEY:$canonicalName", modelClass, *args)
    }

    open operator fun <T : ViewModel> get(key: String, modelClass: Class<T>, vararg args: Any): T {
        var viewModel: ViewModel? = get(key)

        if (modelClass.isInstance(viewModel)) {
            return viewModel as T
        } else {
            if (viewModel == null) {
                Log.w("MyViewModelProvider", "viewModel == null, and create it now")
            }
        }
        viewModel = when (mFactory) {
            is DefaultMyFactory -> mFactory.create(modelClass, *args)
            is MyFactory -> mFactory.create(key, modelClass, *args)
            is KeyedFactory -> mFactory.create(key, modelClass)
            else -> mFactory.create(modelClass)
        }

        set(key, viewModel)
        return viewModel
    }

    open operator fun <T : ViewModel> get(key: String): T? {
        // ViewModel viewModel = mViewModelStore.get(key);
        return try {
            getMethod!!.invoke(mViewModelStore, key) as T?
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Impossible occurred IllegalAccessException in MyViewModelProvider about getMethod", e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Impossible occurred InvocationTargetException in MyViewModelProvider about getMethod", e)
        } catch (e: Exception) {
            return null
        }
    }

    open operator fun <T : ViewModel> set(key: String, viewModel: T) {
        // mViewModelStore.put(key, viewModel);
        try {
            putMethod!!.invoke(mViewModelStore, key, viewModel)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Impossible occurred IllegalAccessException in MyViewModelProvider about putMethod", e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Impossible occurred InvocationTargetException in MyViewModelProvider about putMethod", e)
        }
    }

    abstract inner class MyFactory : Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException("create(Class<?>, Object...) must be called on "
                    + "implementaions of MyFactory")
        }

        abstract fun <T : ViewModel?> create(key: String?, modelClass: Class<T>, vararg args: Any?): T
    }

    class DefaultMyFactory private constructor() : NewInstanceFactory() {
        fun <T : ViewModel> create(modelClass: Class<T>, vararg args: Any): T {
            if (args.isNotEmpty()) {
                val parameterTypes: Array<Class<*>?> = arrayOfNulls(args.size)
                args.indices.forEach { parameterTypes[it] = args[it].javaClass }
                return try {
                    modelClass.getConstructor(*parameterTypes).newInstance(*args)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InstantiationException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                }
            }
            return super.create(modelClass)
        }

        companion object {
            var instance: DefaultMyFactory? = null
                get() {
                    if (field == null) {
                        field = DefaultMyFactory()
                    }
                    return field
                }
                private set
        }
    }
}
