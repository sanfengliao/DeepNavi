@file:Suppress("unused")

package com.sysu.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import java.lang.reflect.Field
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
    }

    constructor(desc: String, type: Type, spKey: String) : super(desc, type) {
        this.spKey = spKey
    }

    open fun getSpKey() = spKey

    open fun saveInSp(sp: SharedPreferences) {
        val spEdit = sp.edit()
        when (getType()) {
            Boolean::class.java -> spEdit.putBoolean(spKey, getData() as? Boolean ?: false)
            Int::class.java -> spEdit.putInt(spKey, getData() as? Int ?: 0)
            Long::class.java -> spEdit.putLong(spKey, getData() as? Long ?: 0L)
            Float::class.java -> spEdit.putFloat(spKey, getData() as? Float ?: 0f)
            Double::class.java -> spEdit.putString(spKey, (getData() as? Double ?: 0.0).toString())
            String::class.java -> spEdit.putString(spKey, getData() as? String ?: "")
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

open class PropertyHolder {
    companion object {
        val filterAction: (Field) -> Boolean =
            { it.type == Property::class.java && Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) }
    }

    private val properties: MutableList<Property<*>> = mutableListOf()

    open fun addProperty(property: Property<*>) = properties.add(property)
    open fun addProperties(properties: List<Property<*>>) = this.properties.addAll(properties)
    open fun removeProperty(property: Property<*>) = properties.remove(property)
    open fun hasProperty(property: Property<*>) = property in properties
    open fun getProperties() = properties
    open fun getPropertyAt(index: Int) = properties[index]
    open fun clearAll() = properties.clear()

    open fun readPropertiesFromClass(cls: Class<*>) = properties.addAll(cls.fields.filter(filterAction).map { it.get(null) as Property<*> })
}

class SpPropertyHelper(private val context: Context, private val spName: String) : PropertyHolder() {
    private var sp: SharedPreferences = context.getSharedPreferences(spName, Context.MODE_PRIVATE)
    private val observeAction: (Any) -> Unit = { saveProperty(it as Property<*>) }
    private val modes = ArrayList<String>()

    override fun addProperty(property: Property<*>): Boolean {
        property.observeForever(observeAction)
        return super.addProperty(property)
    }

    override fun addProperties(properties: List<Property<*>>): Boolean {
        properties.forEach { it.observeForever(observeAction) }
        return super.addProperties(properties)
    }

    override fun readPropertiesFromClass(cls: Class<*>): Boolean {
        val properties = cls.fields.filter(filterAction).map { it.get(null) as Property<*> }
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

    fun getContext() = context
    fun getSpName() = spName
}

class NetPropertyHelper(private val url: String) : PropertyHolder() {
    init {
        TODO()
    }
}
