@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.sysu.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type

// [SharedPerference 里存储StringSet，App关闭丢失数据问题](https://blog.csdn.net/weixin_40299948/article/details/80008940)

open class Property<T : Any> : MutableLiveData<T> {
    protected var mDesc: String
    protected var mData: T? = null
    protected var mType: Type

    constructor(desc: String, data: T) : super(data) {
        this.mDesc = desc
        this.mData = data
        this.mType = data::class.java
    }

    constructor(desc: String, type: Type) : super() {
        this.mDesc = desc
        this.mData = null
        this.mType = type
    }

    open fun getDesc() = mDesc
    open fun getData() = mData
    open fun getType() = mType

    override fun postValue(value: T?) {
        mData = value
        super.postValue(value)
    }

    @MainThread
    override fun setValue(value: T?) {
        mData = value
        super.setValue(value)
    }

    open fun setData(value: T?) {
        this.mData = value
    }
}

open class SpProperty<T : Any> : Property<T> {
    companion object {
        inline fun <reified T : Any> getSpPropertyFromSp(sp: SharedPreferences, spKey: String): SpProperty<T> {
            return if (!sp.contains(spKey)) {
                SpProperty(sp.getString("${spKey}_desc", "") ?: "", T::class.java, spKey)
            } else {
                SpProperty(
                    sp.getString("${spKey}_desc", "") ?: "", when (T::class.java) {
                        Boolean::class.java -> sp.getBoolean(spKey, false) as T
                        Int::class.java -> sp.getInt(spKey, 0) as T
                        Long::class.java -> sp.getLong(spKey, 0L) as T
                        Float::class.java -> sp.getFloat(spKey, 0f) as T
                        Double::class.java -> sp.getString(spKey, "0.0")!!.toDouble() as T
                        String::class.java -> sp.getString(spKey, "") as T
                        else -> JsonApi.fromJsonNonNull(sp.getString(spKey, "") ?: "", T::class.java)
                    }, spKey
                )
            }
        }
    }

    protected val mSpKey: String

    constructor(desc: String, data: T, spKey: String) : super(desc, data) {
        this.mSpKey = spKey
    }

    constructor(desc: String, type: Type, spKey: String) : super(desc, type) {
        this.mSpKey = spKey
    }

    open fun getSpKey() = mSpKey

    @Suppress("UNCHECKED_CAST")
    open fun saveInSp(sp: SharedPreferences) {
        val spEdit = sp.edit()
        val data = mData ?: return
        val type = mType
        when {
            type == Boolean::class.java -> spEdit.putBoolean(mSpKey, data as Boolean)
            type == Int::class.java -> spEdit.putInt(mSpKey, data as Int)
            type == Long::class.java -> spEdit.putLong(mSpKey, data as Long)
            type == Float::class.java -> spEdit.putFloat(mSpKey, data as Float)
            type == Double::class.java -> spEdit.putString(mSpKey, (data as Double).toString())
            type == String::class.java -> spEdit.putString(mSpKey, data as String)
            type is Class<*> && type.isImplementInclusive(Set::class.java) -> {
                val dataSet = data as Set<*>
                if (dataSet.isEmpty()) {
                    return
                }
                if (dataSet.first() is String) {
                    spEdit.putStringSet(mSpKey, dataSet as Set<String>)
                } else {
                    spEdit.putString(mSpKey, JsonApi.toJson(dataSet))
                }
            }
            else -> spEdit.putString(mSpKey, JsonApi.toJson(data))
        }
        spEdit.putString("${mSpKey}_desc", mDesc)
        spEdit.apply()
    }

    @Suppress("UNCHECKED_CAST")
    open fun getFromSp(sp: SharedPreferences) {
        if (sp.contains(mSpKey)) {
            val type = getType()
            mData = when {
                type == Boolean::class.java -> sp.getBoolean(mSpKey, false) as? T
                type == Int::class.java -> sp.getInt(mSpKey, 0) as? T
                type == Long::class.java -> sp.getLong(mSpKey, 0L) as? T
                type == Float::class.java -> sp.getFloat(mSpKey, 0f) as? T
                type == Double::class.java -> sp.getString(mSpKey, "0.0")!!.toDouble() as? T
                type == String::class.java -> sp.getString(mSpKey, "") as? T
                type is Class<*> && type.isImplementInclusive(Set::class.java) -> {
                    val stringSet = sp.getStringSet(mSpKey, null)
                    if (stringSet != null) {
                        HashSet<String>(stringSet) as? T
                    } else {
                        JsonApi.fromJsonOrNull(sp.getString(mSpKey, "") ?: "", type)
                    }
                }
                else -> JsonApi.fromJsonOrNull(sp.getString(mSpKey, "") ?: "", type)
            }
        }
    }

    open fun remove(sp: SharedPreferences) = sp.edit().remove(mSpKey).apply()
}

open class ContextSpProperty<T : Any> : SpProperty<T> {
    private val context: Context
    var mode: Int = Context.MODE_PRIVATE

    constructor(desc: String, data: T, spKey: String, context: Context) : super(desc, data, spKey) {
        this.context = context
    }

    constructor(desc: String, type: Type, spKey: String, context: Context) : super(desc, type, spKey) {
        this.context = context
    }

    override fun setValue(value: T?) {
        if (value != mData) {
            super.setValue(value)
            saveInSp(context.getSharedPreferences(PROPERTY_SP_NAME, mode))
        }
    }

    override fun setData(value: T?) {
        if (value != mData) {
            super.setData(value)
            saveInSp(context.getSharedPreferences(PROPERTY_SP_NAME, mode))
        }
    }

    override fun getValue(): T? {
        if (mData == null) {
            getFromSp(context.getSharedPreferences(PROPERTY_SP_NAME, mode))
        }
        return super.getValue()
    }

    override fun getData(): T? {
        if (mData == null) {
            getFromSp(context.getSharedPreferences(PROPERTY_SP_NAME, mode))
        }
        return super.getData()
    }

    open fun remove() = context.getSharedPreferences(PROPERTY_SP_NAME, mode).edit().remove(mSpKey).apply()

    companion object {
        const val PROPERTY_SP_NAME = "PROPERTY_SP_NAME"
    }
}

open class AppSpProperty<T : Any> : ContextSpProperty<T> {
    constructor(desc: String, data: T, spKey: String) : super(desc, data, spKey, ContextApi.appContext)
    constructor(desc: String, type: Type, spKey: String) : super(desc, type, spKey, ContextApi.appContext)
}

open class NetProperty<T : Any> : Property<T> {
    private var modeValues: MutableMap<String, String>? = null
    private var options: ArrayList<String>? = null

    constructor(desc: String, data: T) : super(desc, data)
    constructor(desc: String, type: Type) : super(desc, type)

    // mode values

    open fun prepareModeValues() {
        this.modeValues = HashMap()
    }

    open fun addModeValue(mode: String, value: String) = this.modeValues?.put(mode, value)
    open fun removeModeValue(mode: String) = this.modeValues?.remove(mode)
    open fun hasModeValue(mode: String): Boolean = this.modeValues?.containsKey(mode) ?: false
    open fun getModeValue(mode: String) = this.modeValues?.get(mode)

    // optionValues

    open fun prepareOptions() {
        this.options = ArrayList()
    }

    open fun addOption(option: String) = this.options?.add(option)
    open fun removeOption(option: String) = this.options?.remove(option)
    open fun getOptions() = this.options?.toList() ?: listOf()
    @SuppressLint("CI_ByteDanceKotlinRules_List_Contains_Not_Allow")
    open fun hasOption(option: String): Boolean = this.options?.contains(option) ?: false
}

open class PropertyHolder : ViewModel() {
    companion object {
        val fieldFilterAction: (Field) -> Boolean = {
            it.type == Property::class.java &&
                    Modifier.isPublic(it.modifiers) &&
                    Modifier.isStatic(it.modifiers) &&
                    Modifier.isFinal(it.modifiers)
        }
        val methodFilterAction: (Method) -> Boolean = {
            it.returnType == Property::class.java &&
                    Modifier.isPublic(it.modifiers) &&
                    Modifier.isStatic(it.modifiers) &&
                    Modifier.isFinal(it.modifiers)
        }
    }

    private val properties: MutableList<Property<*>> = mutableListOf()

    open fun addProperty(property: Property<*>) = properties.add(property)
    open fun addProperties(properties: List<Property<*>>) = this.properties.addAll(properties)
    open fun removeProperty(property: Property<*>) = properties.remove(property)
    open fun hasProperty(property: Property<*>) = property in properties
    open fun getProperties() = properties
    open fun getPropertyAt(index: Int) = properties[index]
    open fun clearAll() = properties.clear()

    open fun readPropertiesFromClass(cls: Class<*>) =
        properties.run {
            addAll(cls.fields.filter(fieldFilterAction).map { it.get(null) as Property<*> }) &&
                    addAll(cls.methods.filter(methodFilterAction).map { it.invoke(null) as Property<*> })
        }
}

open class SpPropertyHelper(protected val mContext: Context, protected val mSpName: String) : PropertyHolder() {
    private var sp: SharedPreferences = mContext.getSharedPreferences(mSpName, Context.MODE_PRIVATE)
    private val observeAction: (Any) -> Unit = { saveProperty(it as Property<*>) }

    override fun addProperty(property: Property<*>): Boolean {
        property.observeForever(observeAction)
        return super.addProperty(property)
    }

    override fun addProperties(properties: List<Property<*>>): Boolean {
        properties.forEach { it.observeForever(observeAction) }
        return super.addProperties(properties)
    }

    override fun readPropertiesFromClass(cls: Class<*>): Boolean {
        val properties = mutableListOf<Property<*>>()
        properties.addAll(cls.fields.filter(fieldFilterAction).map { it.get(null) as Property<*> })
        properties.addAll(cls.methods.filter(methodFilterAction).map { it.invoke(null) as Property<*> })
        properties.forEach { it.observeForever(observeAction) }
        return super.addProperties(properties)
    }

    override fun removeProperty(property: Property<*>): Boolean {
        property.removeObserver(observeAction)
        return super.removeProperty(property)
    }

    override fun clearAll() {
        getProperties().forEach { it.removeObserver(observeAction) }
        super.clearAll()
    }

    fun saveProperty(property: Property<*>) {
        if (property is SpProperty) {
            property.saveInSp(sp)
        }
    }

    fun getContext() = mContext
    fun getSpName() = mSpName
}

open class NetPropertyHelper(protected val url: String) : PropertyHolder() {
    init {
        TODO("NetPropertyHelper hasn't implemented")
    }
}
