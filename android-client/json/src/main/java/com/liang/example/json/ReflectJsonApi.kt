@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.liang.example.json

import java.lang.reflect.Array as RArray
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Array<Unit> -> 0
 * BooleanArray -> 1 / Array<Boolean> -> 11
 * ByteArray -> 2 / Array<Byte> -> 12
 * ShortArray -> 3 / Array<Short> -> 13
 * IntArray -> 4 / Array<Int> -> 14
 * LongArray -> 5 / Array<Long> -> 15
 * FloatArray -> 6 / Array<Float> -> 16
 * DoubleArray -> 7 / Array<Double> -> 17
 * CharArray -> 8 / Array<Char> -> 18
 * Array<String> -> 9
 * Array<Any> -> 10
 * Array<BigInteger> -> 19
 * Array<BigDecimal> -> 20
 * @return Pair<type: Int, depth: Int>
 */
fun findItemTypeByCls(cls: Class<*>): Pair<Int, Int> {
    val clsStr = cls.toString()
    if (!clsStr.startsWith("class [")) {
        throw RuntimeException("cls should be array's cls")
    }
    var index = 7
    var depth = 0
    while (index < clsStr.length && clsStr[index] == '[') {
        index++
        depth++
    }
    val temp1 = clsStr[index]
    val temp2 = clsStr.substring(index + 1)
    return Pair(
            when {
                temp2.isEmpty() -> when (temp1) {
                    'Z' -> 1
                    'B' -> 2
                    'S' -> 3
                    'I' -> 4
                    'J' -> 5
                    'F' -> 6
                    'D' -> 7
                    'C' -> 8
                    else -> 0
                }
                temp2.startsWith("java.lang.") -> when (temp2) {
                    "java.lang.String;" -> 9
                    "java.lang.Boolean;" -> 11
                    "java.lang.Byte;" -> 12
                    "java.lang.Short;" -> 13
                    "java.lang.Integer;" -> 14
                    "java.lang.Long;" -> 15
                    "java.lang.Float;" -> 16
                    "java.lang.Double;" -> 17
                    "java.lang.Char;" -> 18
                    else -> 10
                }
                temp2 == "kotlin.Unit;" -> 0
                temp2 == "java.math.BigInteger;" -> 19
                temp2 == "java.math.BigDecimal;" -> 20
                temp1 == 'L' -> 10
                else -> throw RuntimeException("incorrect array cls")
            }, depth
    )
}

interface ReflectHandleInter<T> {
    fun handleBoolean(obj: Any?, f: Field?, v: Boolean?): T?
    fun handleByte(obj: Any?, f: Field?, v: Byte?): T?
    fun handleShort(obj: Any?, f: Field?, v: Short?): T?
    fun handleInt(obj: Any?, f: Field?, v: Int?): T?
    fun handleLong(obj: Any?, f: Field?, v: Long?): T?
    fun handleFloat(obj: Any?, f: Field?, v: Float?): T?
    fun handleDouble(obj: Any?, f: Field?, v: Double?): T?
    fun handleChar(obj: Any?, f: Field?, v: Char?): T?

    fun handleString(obj: Any?, f: Field?, v: String?): T?
    fun handleBigInteger(obj: Any?, f: Field?, v: BigInteger?): T?
    fun handleBigDecimal(obj: Any?, f: Field?, v: BigDecimal?): T?

    fun handleBooleanArray(obj: Any?, f: Field?, v: Array<Boolean?>?): T?
    fun handleByteArray(obj: Any?, f: Field?, v: Array<Byte?>?): T?
    fun handleShortArray(obj: Any?, f: Field?, v: Array<Short?>?): T?
    fun handleIntArray(obj: Any?, f: Field?, v: Array<Int?>?): T?
    fun handleLongArray(obj: Any?, f: Field?, v: Array<Long?>?): T?
    fun handleFloatArray(obj: Any?, f: Field?, v: Array<Float?>?): T?
    fun handleDoubleArray(obj: Any?, f: Field?, v: Array<Double?>?): T?
    fun handleCharArray(obj: Any?, f: Field?, v: Array<Char?>?): T?

    fun handleStringArray(obj: Any?, f: Field?, v: Array<String?>?): T?
    fun handleBigIntegerArray(obj: Any?, f: Field?, v: Array<BigInteger?>?): T?
    fun handleBigDecimalArray(obj: Any?, f: Field?, v: Array<BigDecimal?>?): T?

    fun handleArray(obj: Any?, f: Field?, v: Array<*>?): T?
    fun handleObject(obj: Any?, f: Field?, v: Any?): T?

    @Suppress("UNCHECKED_CAST")
    fun handleArrayInner(array: Array<*>?, transform2: (List<T?>) -> T?): T? {
        if (array.isNullOrEmpty()) {
            return null
        }
        val temp = findItemTypeByCls(array::class.java)
        val itemType = temp.first
        val depth = temp.second
        return when (itemType) {
            in 11..18 -> when (itemType) {
                11 -> transformArray<Boolean>(array, depth, { handleBooleanArray(null, null, it) }, transform2)
                12 -> transformArray<Byte>(array, depth, { handleByteArray(null, null, it) }, transform2)
                13 -> transformArray<Short>(array, depth, { handleShortArray(null, null, it) }, transform2)
                14 -> transformArray<Int>(array, depth, { handleIntArray(null, null, it) }, transform2)
                15 -> transformArray<Long>(array, depth, { handleLongArray(null, null, it) }, transform2)
                16 -> transformArray<Float>(array, depth, { handleFloatArray(null, null, it) }, transform2)
                17 -> transformArray<Double>(array, depth, { handleDoubleArray(null, null, it) }, transform2)
                18 -> transformArray<Char>(array, depth, { handleCharArray(null, null, it) }, transform2)
                else -> null
            }

            in 1..8 -> when (itemType) {
                1 -> transformArray2<BooleanArray>(array, depth, { handleBooleanArray(null, null, it?.toTypedArray() as? Array<Boolean?>) }, transform2)
                2 -> transformArray2<ByteArray>(array, depth, { handleByteArray(null, null, it?.toTypedArray() as? Array<Byte?>) }, transform2)
                3 -> transformArray2<ShortArray>(array, depth, { handleShortArray(null, null, it?.toTypedArray() as? Array<Short?>) }, transform2)
                4 -> transformArray2<IntArray>(array, depth, { handleIntArray(null, null, it?.toTypedArray() as? Array<Int?>) }, transform2)
                5 -> transformArray2<LongArray>(array, depth, { handleLongArray(null, null, it?.toTypedArray() as? Array<Long?>) }, transform2)
                6 -> transformArray2<FloatArray>(array, depth, { handleFloatArray(null, null, it?.toTypedArray() as? Array<Float?>) }, transform2)
                7 -> transformArray2<DoubleArray>(array, depth, { handleDoubleArray(null, null, it?.toTypedArray() as? Array<Double?>) }, transform2)
                8 -> transformArray2<CharArray>(array, depth, { handleCharArray(null, null, it?.toTypedArray() as? Array<Char?>) }, transform2)
                else -> null
            }

            9 -> transformArray<String>(array, depth, { handleStringArray(null, null, it) }, transform2)
            10 -> transformArray2<Any>(array, depth, { handleObject(null, null, it) }, transform2)
            19 -> transformArray<BigInteger>(array, depth, { handleBigIntegerArray(null, null, it) }, transform2)
            20 -> transformArray<BigDecimal>(array, depth, { handleBigDecimalArray(null, null, it) }, transform2)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <I> transformArray(array: Array<*>?, depth: Int, transform: (Array<I?>) -> T?, transform2: (List<T?>) -> T?): T? = when {
        array.isNullOrEmpty() -> null
        depth == 0 -> transform(array as Array<I?>)
        else -> {
            val nextDepth = depth - 1
            transform2(array.map { transformArray(it as? Array<*>, nextDepth, transform, transform2) })
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <I> transformArray2(array: Array<*>?, depth: Int, transform: (I?) -> T?, transform2: (List<T?>) -> T?): T? = when {
        array.isNullOrEmpty() -> null
        depth == 0 -> transform2((array as Array<I?>).map(transform))
        else -> {
            val nextDepth = depth - 1
            transform2(array.map { transformArray2(it as? Array<*>, nextDepth, transform, transform2) })
        }
    }

    fun handleByCls(cls: Class<*>, v: Any): T? {
        val str = cls.toString()
        return when {
            !str.startsWith("class ") -> when (cls) {
                Boolean::class.java -> handleBoolean(null, null, v as? Boolean)
                Byte::class.java -> handleByte(null, null, v as? Byte)
                Short::class.java -> handleShort(null, null, v as? Short)
                Int::class.java -> handleInt(null, null, v as? Int)
                Long::class.java -> handleLong(null, null, v as? Long)
                Float::class.java -> handleFloat(null, null, v as? Float)
                Double::class.java -> handleDouble(null, null, v as? Double)
                Char::class.java -> handleChar(null, null, v as? Char)  // 9次判断
                else -> throw RuntimeException("unknown class type")
            }
            cls == String::class.java -> handleString(null, null, v as? String)
            cls == BigInteger::class.java -> handleBigInteger(null, null, v as? BigInteger)
            cls == BigDecimal::class.java -> handleBigDecimal(null, null, v as? BigDecimal)
            str.startsWith("class [") -> dispatchArrTask(cls, v as? Array<*>, null, null)
            else -> handleObject(null, null, v)
        }
    }

    fun handleByField(it: Field, obj: Any): T? {
        val cls = it.type
        val str = cls.toString()
        return when {
            !str.startsWith("class ") -> when (cls) {
                Boolean::class.java -> handleBoolean(obj, it, it.getBoolean(obj))
                Byte::class.java -> handleByte(obj, it, it.getByte(obj))
                Short::class.java -> handleShort(obj, it, it.getShort(obj))
                Int::class.java -> handleInt(obj, it, it.getInt(obj))
                Long::class.java -> handleLong(obj, it, it.getLong(obj))
                Float::class.java -> handleFloat(obj, it, it.getFloat(obj))
                Double::class.java -> handleDouble(obj, it, it.getDouble(obj))
                Char::class.java -> handleChar(obj, it, it.getChar(obj))  // 9次判断
                else -> throw RuntimeException("unknown class type")
            }
            cls == String::class.java -> handleString(obj, it, it.get(obj) as? String)
            cls == BigInteger::class.java -> handleBigInteger(null, null, it.get(obj) as? BigInteger)
            cls == BigDecimal::class.java -> handleBigDecimal(null, null, it.get(obj) as? BigDecimal)
            str.startsWith("class [") -> dispatchArrTask(cls, it.get(obj), obj, it)
            else -> handleObject(obj, it, it.get(obj))
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun dispatchArrTask(cls: Class<*>, v: Any?, obj: Any?, f: Field?): T? {
        if (v == null) {
            return null
        }
        return when (cls) {
            BooleanArray::class.java -> handleBooleanArray(obj, f, (v as BooleanArray).toTypedArray() as Array<Boolean?>)
            ByteArray::class.java -> handleByteArray(obj, f, (v as ByteArray).toTypedArray() as Array<Byte?>)
            ShortArray::class.java -> handleShortArray(obj, f, (v as ShortArray).toTypedArray() as Array<Short?>)
            IntArray::class.java -> handleIntArray(obj, f, (v as IntArray).toTypedArray() as Array<Int?>)
            LongArray::class.java -> handleLongArray(obj, f, (v as LongArray).toTypedArray() as Array<Long?>)
            FloatArray::class.java -> handleFloatArray(obj, f, (v as FloatArray).toTypedArray() as Array<Float?>)
            DoubleArray::class.java -> handleDoubleArray(obj, f, (v as DoubleArray).toTypedArray() as Array<Double?>)
            CharArray::class.java -> handleCharArray(obj, f, (v as CharArray).toTypedArray() as Array<Char?>)
            else -> handleArray(obj, f, v as Array<*>)
        }
    }
}

interface JsonInter {
    fun <T> fromJson(jsonStr: String, cls: Class<*>): T
    fun <T> fromJsonOrNull(jsonStr: String, cls: Class<*>): T?
    fun <T> fromJsonOrNull(jsonVal: SimpleJsonValue<*>, cls: Class<*>): T?
    fun <T : Any> toJson(obj: T): String
    fun <T : Any> toJsonOrNull(obj: T): String?
}

// 最垃圾的缓存机制，没有定时清理，没有时间记录
fun getFieldsFromCls(clsCache: MutableMap<Class<*>, Array<Field>>, cls: Class<*>): Array<Field> {
    var result = clsCache[cls]
    if (result == null) {
        result = cls.declaredFields.filter { !Modifier.isStatic(it.modifiers) }.toTypedArray()
        result.forEach { it.isAccessible = true }
    }
    return result
}

fun getDefaultCtorFromCls(ctorCache: MutableMap<Class<*>, Constructor<*>>, cls: Class<*>): Constructor<*> {
    var result = ctorCache[cls]
    if (result == null) {
        result = cls.getConstructor()
        result.isAccessible = true
    }
    return result
}

enum class JsonArrayDimenStrategy {
    GET_FIRST,
    CALCULATE,
}

open class ReflectJsonApi(
    var strategy: JsonStrategy = JsonStrategy.SIMPLEST,
    var style: JsonStyle = JsonStyle.STANDARD,
    var arrayStrategy: JsonArrayDimenStrategy = JsonArrayDimenStrategy.CALCULATE,
    val clsCache: MutableMap<Class<*>, Array<Field>> = mutableMapOf(),
    val ctorCache: MutableMap<Class<*>, Constructor<*>> = mutableMapOf()
) : JsonInter {
    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJson(jsonStr: String, cls: Class<*>): T =
            ReflectFromJsonTask(arrayStrategy, clsCache, ctorCache)
                .fromJsonValue(SimpleJsonParser.fromJson(jsonStr, style), cls) as T

    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJsonOrNull(jsonStr: String, cls: Class<*>): T? =
            ReflectFromJsonTask(arrayStrategy, clsCache, ctorCache)
                .fromJsonValue(SimpleJsonParser.fromJson(jsonStr, style), cls) as? T

    @Suppress("UNCHECKED_CAST")
    override fun <T> fromJsonOrNull(jsonVal: SimpleJsonValue<*>, cls: Class<*>): T? =
        ReflectFromJsonTask(arrayStrategy, clsCache, ctorCache).fromJsonValue(jsonVal, cls) as? T

    override fun <T : Any> toJson(obj: T): String {
        val result = ReflectToJsonTask(clsCache).handleByCls(obj::class.java, obj) ?: return ""
        if (result is SimpleJsonArray) {
            result.setRightStrategy(strategy)
        }
        if (result is SimpleJsonObject) {
            result.setRightStrategy(strategy)
        }
        return result.string()
    }

    override fun <T : Any> toJsonOrNull(obj: T): String? {
        val result = ReflectToJsonTask(clsCache).handleByCls(obj::class.java, obj) ?: return null
        if (result is SimpleJsonArray) {
            result.setRightStrategy(strategy)
        }
        if (result is SimpleJsonObject) {
            result.setRightStrategy(strategy)
        }
        return result.string()
    }

    open class ReflectToJsonTask(val clsCache: MutableMap<Class<*>, Array<Field>>) :
        ReflectHandleInter<SimpleJsonValue<*>> {
        // basic

        override fun handleBoolean(obj: Any?, f: Field?, v: Boolean?): SimpleJsonBoolean? = if (v != null) SimpleJsonBoolean(
            v
        ) else null
        override fun handleByte(obj: Any?, f: Field?, v: Byte?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleShort(obj: Any?, f: Field?, v: Short?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleInt(obj: Any?, f: Field?, v: Int?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleLong(obj: Any?, f: Field?, v: Long?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleFloat(obj: Any?, f: Field?, v: Float?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleDouble(obj: Any?, f: Field?, v: Double?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleChar(obj: Any?, f: Field?, v: Char?): SimpleJsonNumber? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleBigInteger(obj: Any?, f: Field?, v: BigInteger?): SimpleJsonValue<*>? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleBigDecimal(obj: Any?, f: Field?, v: BigDecimal?): SimpleJsonValue<*>? = if (v != null) SimpleJsonNumber(
            v
        ) else null
        override fun handleString(obj: Any?, f: Field?, v: String?): SimpleJsonString? = if (v != null) SimpleJsonString(
            v
        ) else null

        // basicArray

        override fun handleBooleanArray(obj: Any?, f: Field?, v: Array<Boolean?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonBoolean(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleByteArray(obj: Any?, f: Field?, v: Array<Byte?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleShortArray(obj: Any?, f: Field?, v: Array<Short?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleIntArray(obj: Any?, f: Field?, v: Array<Int?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleLongArray(obj: Any?, f: Field?, v: Array<Long?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleFloatArray(obj: Any?, f: Field?, v: Array<Float?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleDoubleArray(obj: Any?, f: Field?, v: Array<Double?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleCharArray(obj: Any?, f: Field?, v: Array<Char?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null


        override fun handleStringArray(obj: Any?, f: Field?, v: Array<String?>?): SimpleJsonArray? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonString(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleBigIntegerArray(obj: Any?, f: Field?, v: Array<BigInteger?>?): SimpleJsonValue<*>? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        override fun handleBigDecimalArray(obj: Any?, f: Field?, v: Array<BigDecimal?>?): SimpleJsonValue<*>? =
                if (v?.isNotEmpty() == true) SimpleJsonArray(v.map {
                    if (it != null) SimpleJsonNumber(
                        it
                    ) else null
                }.toMutableList()) else null

        // array: char[][]
        override fun handleArray(obj: Any?, f: Field?, v: Array<*>?): SimpleJsonValue<*>? = handleArrayInner(v) {
            SimpleJsonArray(
                it.toMutableList()
            )
        }

        // object
        override fun handleObject(obj: Any?, f: Field?, v: Any?): SimpleJsonValue<*>? {
            if (v == null) {
                return null
            }
            val fields = getFieldsFromCls(clsCache, v::class.java)
            if (fields.isEmpty()) {
                return null
            }
            val itemMap = mutableMapOf<String, SimpleJsonValue<*>?>()
            fields.forEach { itemMap[it.name] = handleByField(it, v) ?: JSON_NULL }
            return SimpleJsonObject(itemMap)
        }
    }

    open class ReflectFromJsonTask(
        var arrayStrategy: JsonArrayDimenStrategy = JsonArrayDimenStrategy.CALCULATE,
        val clsCache: MutableMap<Class<*>, Array<Field>>, val ctorCache: MutableMap<Class<*>, Constructor<*>>
    ) {
        open fun fromJsonValue(jsonValue: SimpleJsonValue<*>, cls: Class<*>?): Any? {
            if (jsonValue.value() == null) {
                return null
            }
            return when (val type = jsonValue.type()) {
                JsonType.ARRAY -> fromJsonArray(jsonValue as SimpleJsonArray, cls)
                JsonType.OBJECT -> fromJsonObject(jsonValue as SimpleJsonObject, cls)
                JsonType.NUL -> null
                JsonType.NUMBER -> {
                    val n = jsonValue as SimpleJsonNumber
                    if (n.numberType() == JsonNumberType.CHAR) n.charValue() else n.value2()
                }
                JsonType.BOOL, JsonType.STRING -> jsonValue.value2()
                else -> throw RuntimeException("jsonValue's type is wrong: $type")
            }
        }

        @Suppress("UNCHECKED_CAST")
        open fun fromJsonArray(jsonArray: SimpleJsonArray, cls: Class<*>?): Any? {
            if (jsonArray.value().isNullOrEmpty() || cls == null) {
                return null
            }
            val dimens = when (arrayStrategy) {
                JsonArrayDimenStrategy.CALCULATE -> getDimensFromJsonArray2(jsonArray)
                JsonArrayDimenStrategy.GET_FIRST -> getDimensFromJsonArray(jsonArray)
            }
            if (dimens == null || dimens.isEmpty() || dimens[0] == 0) {
                return null
            }
            val temp = findItemTypeByCls(cls)
            val itemType = temp.first
            val depth = temp.second
            // println("\tcreate array -- itemType: $itemType, depth: $depth, dimens: [${dimens.joinToString()}]")
            return when (itemType) {
                in 1..8 -> when (itemType) {
                    1 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Boolean::class.java, *dimens))
                    { i, v, a -> RArray.setBoolean(a, i, v.value() as Boolean) }
                    2 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Byte::class.java, *dimens))
                    { i, v, a -> RArray.setByte(a, i, (v.value() as Long).toByte()) }
                    3 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Short::class.java, *dimens))
                    { i, v, a -> RArray.setShort(a, i, (v.value() as Long).toShort()) }
                    4 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Int::class.java, *dimens))
                    { i, v, a -> RArray.setInt(a, i, (v.value() as Long).toInt()) }
                    5 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Long::class.java, *dimens))
                    { i, v, a -> RArray.setLong(a, i, v.value() as Long) }
                    6 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Float::class.java, *dimens))
                    { i, v, a -> RArray.setFloat(a, i, (v.value() as Double).toFloat()) }
                    7 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Double::class.java, *dimens))
                    { i, v, a -> RArray.setDouble(a, i, v.value() as Double) }
                    8 -> transformJsonArray(jsonArray, depth, RArray.newInstance(Char::class.java, *dimens))
                    { i, v, a -> RArray.setChar(a, i, (v.value() as Long).toChar()) }
                    else -> null
                }

                in 11..18 -> when (itemType) {
                    11 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Boolean::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, v.value() as Boolean) }
                    12 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Byte::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, (v.value() as Long).toByte()) }
                    13 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Short::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, (v.value() as Long).toShort()) }
                    14 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Integer::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, (v.value() as Long).toInt()) }
                    15 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Long::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, v.value() as Long) }
                    16 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Float::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, (v.value() as Double).toFloat()) }
                    17 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Double::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, v.value() as Double) }
                    18 -> transformJsonArray(jsonArray, depth, RArray.newInstance(java.lang.Character::class.java, *dimens))
                    { i, v, a -> RArray.set(a, i, (v.value() as Long).toChar()) }
                    else -> null
                }

                9 -> transformJsonArray(jsonArray, depth, RArray.newInstance(String::class.java, *dimens))
                { i, v, a -> RArray.set(a, i, v.value() as String) }
                19 -> transformJsonArray(jsonArray, depth, RArray.newInstance(BigInteger::class.java, *dimens))
                { i, v, a -> RArray.set(a, i, v.value() as BigInteger) }
                20 -> transformJsonArray(jsonArray, depth, RArray.newInstance(BigDecimal::class.java, *dimens))
                { i, v, a -> RArray.set(a, i, v.value() as BigDecimal) }

                10 -> {
                    val clsStr = cls.toString()
                    val finalCls = Class.forName(clsStr.substring(clsStr.lastIndexOf('[') + 2, clsStr.length - 1))
                    transformJsonArray(jsonArray, depth, RArray.newInstance(finalCls, *dimens)) transformJsonArray@{ i, v, a ->
                        val objValue = fromJsonObject(v as SimpleJsonObject, finalCls) ?: return@transformJsonArray
                        RArray.set(a, i, finalCls.cast(objValue))
                    }
                }
                0 -> null
                else -> throw RuntimeException("itemType should not be $itemType, now the depth is $depth")
            }
        }

        open fun transformJsonArray(
            jsonArray: SimpleJsonArray?, depth: Int, arrayObj: Any,
            transform: (index: Int, value: SimpleJsonValue<*>, arrayObj: Any) -> Unit
        ): Any {
            when {
                jsonArray == null || jsonArray.value().isNullOrEmpty() -> Unit
                depth == 0 -> jsonArray.value2().forEachIndexed { index, jsonValue ->
                    if (jsonValue?.value() != null) {
                        transform(index, jsonValue, arrayObj)
                    }
                }
                else -> {
                    val nextDepth = depth - 1
                    jsonArray.value2().forEachIndexed { index, nextArray ->
                        transformJsonArray(nextArray as? SimpleJsonArray, nextDepth, RArray.get(arrayObj, index) as Any, transform)
                    }
                }
            }
            return arrayObj
        }

        // 计算每个维度的值，取首个
        open fun getDimensFromJsonArray(jsonArray: SimpleJsonArray?): IntArray? {
            if (jsonArray?.value() == null) {
                return null
            }
            var values = jsonArray.value2()
            if (values.isEmpty()) {
                return null
            }
            val dimens = mutableListOf<Int>()
            while (true) {
                dimens.add(values.size)
                val firstItem = values[0]!!
                if (firstItem is SimpleJsonArray) {
                    values = firstItem.value2()
                    if (values.isEmpty()) {
                        return null
                    }
                } else {
                    break
                }
            }
            return dimens.toIntArray()
        }

        // 计算每个维度的最大值那种
        open fun getDimensFromJsonArray2(jsonArray: SimpleJsonArray?): IntArray? {
            if (jsonArray?.value() == null) {
                return null
            }
            val values = jsonArray.value2()
            if (values.isEmpty()) {
                return null
            }
            val dimens = mutableListOf<Int>()
            dimens.add(values.size)
            var maxDepth = 0
            val allSubDimens = values.mapNotNull {
                if (it is SimpleJsonArray) {
                    val subDimens = getDimensFromJsonArray2(it)
                    if (subDimens != null && subDimens.size > maxDepth) {
                        maxDepth = subDimens.size
                    }
                    subDimens
                } else {
                    null
                }
            }
            (0 until maxDepth).forEach { index ->
                var dimen = 0
                allSubDimens.forEach { subDimens ->
                    if (subDimens.size > index && subDimens[index] > dimen) {
                        dimen = subDimens[index]
                    }
                }
                if (dimen == 0) {
                    return null
                }
                dimens.add(dimen)
            }
            return dimens.toIntArray()
        }

        open fun fromJsonObject(jsonObject: SimpleJsonObject?, cls: Class<*>?): Any? {
            if (jsonObject == null || jsonObject.value().isNullOrEmpty() || cls == null) {
                return null
            }
            val fields = getFieldsFromCls(clsCache, cls)
            val result = getDefaultCtorFromCls(ctorCache, cls).newInstance()
            jsonObject.value2().forEach {
                val field = fields.find { f -> f.name == it.key } ?: return@forEach
                val jsonValueItem = it.value ?: return@forEach
                if (jsonValueItem.value() == null) {
                    return@forEach
                }
                val fieldCls = field.type
                val fieldClsStr = fieldCls.toString()
                val jsonValueType = jsonValueItem.type()
                // println("field -- name: ${field.name}, cls: $fieldClsStr, type: $jsonValueType")
                when {
                    fieldCls == Unit::class.java && jsonValueType == JsonType.NUL -> Unit
                    fieldCls == Boolean::class.java && jsonValueType == JsonType.BOOL -> field.setBoolean(result, jsonValueItem.value() as Boolean)
                    jsonValueType == JsonType.NUMBER -> {
                        val n = jsonValueItem as SimpleJsonNumber
                        n.numberType()
                        when (fieldCls) {
                            Byte::class.java -> field.setByte(result, n.value()!!.toByte())
                            Short::class.java -> field.setShort(result, n.value()!!.toShort())
                            Int::class.java -> field.setInt(result, n.value()!!.toInt())
                            Long::class.java -> field.setLong(result, n.value()!!.toLong())
                            Float::class.java -> field.setFloat(result, n.value()!!.toFloat())
                            Double::class.java -> field.setDouble(result, n.value()!!.toDouble())
                            Char::class.java -> field.setChar(result, n.charValue())
                            BigInteger::class.java -> field.set(result, n.value())
                            BigDecimal::class.java -> field.set(result, n.value())
                            else -> throw RuntimeException("should not get unknown number type from json string")
                        }
                    }
                    fieldCls == String::class.java && jsonValueType == JsonType.STRING -> field.set(result, jsonValueItem.value())
                    fieldClsStr.startsWith("class ") && jsonValueType == JsonType.OBJECT ->
                        field.set(result, fromJsonObject(jsonValueItem as SimpleJsonObject, fieldCls))
                    fieldClsStr.startsWith("class [") && jsonValueType == JsonType.ARRAY ->
                        field.set(result, fromJsonArray(jsonValueItem as SimpleJsonArray, fieldCls))
                    else -> throw RuntimeException("non corresponding jsonValue's type and field's cls -- cls: $fieldClsStr, type: $jsonValueType")
                }
            }
            return result
        }
    }

    companion object {
        fun <T> fromJson(jsonStr: String, cls: Class<*>): T = DEFAULT_REFLECT_JSON_API.fromJson(jsonStr, cls)
        fun <T> fromJsonOrNull(jsonStr: String, cls: Class<*>): T? = DEFAULT_REFLECT_JSON_API.fromJsonOrNull(jsonStr, cls)
        fun <T> fromJsonOrNull(jsonVal: SimpleJsonValue<*>, cls: Class<*>): T? = DEFAULT_REFLECT_JSON_API.fromJsonOrNull(jsonVal, cls)
        fun <T : Any> toJson(obj: T) = DEFAULT_REFLECT_JSON_API.toJson(obj)
        fun <T : Any> toJsonOrNull(obj: T) = DEFAULT_REFLECT_JSON_API.toJsonOrNull(obj)
    }
}

val DEFAULT_REFLECT_JSON_API = ReflectJsonApi()

// TODO: 缓存机制 / SimpleJsonComment
