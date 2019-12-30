@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.sysu.example

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

open class Property<T : Any> : MutableLiveData<T> {
    private var desc: String
    private var data: T? = null
    private var type: Type

    constructor(desc: String, data: T) : super(data) {
        this.desc = desc
        this.data = data
        this.type = data::class.java
    }

    constructor(desc: String, type: Type) : super() {
        this.desc = desc
        this.data = null
        this.type = type
    }

    open fun getDesc() = desc
    open fun getData() = data
    open fun getType() = type

    override fun postValue(value: T) {
        data = value
        super.postValue(value)
    }

    @MainThread
    override fun setValue(value: T) {
        data = value
        super.setValue(value)
    }

    open fun setData(value: T) {
        this.data = value
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

    private val spKey: String

    constructor(desc: String, data: T, spKey: String) : super(desc, data) {
        this.spKey = spKey
        getFromSp()
    }

    constructor(desc: String, type: Type, spKey: String) : super(desc, type) {
        this.spKey = spKey
        getFromSp()
    }

    open fun getSpKey() = spKey

    @Suppress("UNCHECKED_CAST")
    open fun saveInSp(sp: SharedPreferences) {
        val spEdit = sp.edit()
        val data = getData() ?: return
        when (getType()) {
            Boolean::class.java -> spEdit.putBoolean(spKey, data as Boolean)
            Int::class.java -> spEdit.putInt(spKey, data as Int)
            Long::class.java -> spEdit.putLong(spKey, data as Long)
            Float::class.java -> spEdit.putFloat(spKey, data as Float)
            Double::class.java -> spEdit.putString(spKey, (data as Double).toString())
            String::class.java -> spEdit.putString(spKey, data as String)
            Set::class.java -> {
                val setData = data as Set<*>
                if (setData.isEmpty()) {
                    return
                }
                if (setData.first() is String) {
                    spEdit.putStringSet(spKey, setData as Set<String>)
                } else {
                    spEdit.putString(spKey, JsonApi.toJson(setData))
                }
            }
            else -> spEdit.putString(spKey, JsonApi.toJson(getData()))
        }
        spEdit.putString("${spKey}_desc", getDesc())
        spEdit.apply()
    }

    @Suppress("UNCHECKED_CAST")
    open fun getFromSp(sp: SharedPreferences) {
        if (sp.contains(spKey)) {
            setData(
                when (getType()) {
                    Boolean::class.java -> sp.getBoolean(spKey, false) as T
                    Int::class.java -> sp.getInt(spKey, 0) as T
                    Long::class.java -> sp.getLong(spKey, 0L) as T
                    Float::class.java -> sp.getFloat(spKey, 0f) as T
                    Double::class.java -> sp.getString(spKey, "0.0")!!.toDouble() as T
                    String::class.java -> sp.getString(spKey, "") as T
                    else -> JsonApi.fromJsonNonNull(sp.getString(spKey, "") ?: "", getType())
                }
            )
        }
    }

    override fun postValue(value: T) {
        saveInSp()
        super.postValue(value)
    }

    override fun setValue(value: T) {
        saveInSp()
        super.setValue(value)
    }
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
        TODO()
    }
}
