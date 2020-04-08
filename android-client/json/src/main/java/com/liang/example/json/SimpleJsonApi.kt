@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.liang.example.json

import java.lang.Exception
import java.math.BigDecimal
import java.math.BigInteger

enum class JsonStyle {
    STANDARD,
    COMMENTS,
}

enum class JsonType {
    NUL,
    BOOL,
    NUMBER,
    STRING,
    ARRAY,
    OBJECT/*,
    COMMENT*/;

    fun isObject() = this == OBJECT
    fun isArray() = this == ARRAY
    fun isArrayOrObject() = this == ARRAY || this == OBJECT
}

enum class JsonNumberType {
    BYTE,
    SHORT,
    INT,
    LONG,
    BIG_INTEGER,
    FLOAT,
    DOUBLE,
    BIG_DECIMAL,
    CHAR,
    UNKNOWN,
}

enum class JsonStrategy {
    SIMPLEST,
    USE_NULL,
    USE_BLANK,
    USE_NULL_AND_BLANK;

    fun useNull(): Boolean = this == USE_NULL_AND_BLANK || this == USE_NULL
    fun useBLANK(): Boolean = this == USE_NULL_AND_BLANK || this == USE_BLANK
}

interface SimpleJsonValue<T : Any> : Cloneable {
    fun type(): JsonType
    fun value(): T?
    fun value2(): T
    fun string(): String
    fun json(): SimpleJsonApi
    public override fun clone(): SimpleJsonValue<T>
}

abstract class SimpleJsonValueAdapter<T : Any>(var mValue: T?, protected val mType: JsonType) :
    SimpleJsonValue<T> {
    protected var mJsonApi: SimpleJsonApi? = null
    override fun type(): JsonType = mType
    override fun value(): T? = mValue
    override fun value2(): T = mValue!!
    override fun string(): String = mValue?.toString() ?: "null"
    override fun json(): SimpleJsonApi {
        if (mJsonApi == null) {
            mJsonApi = SimpleJsonApi(this)
        }
        return mJsonApi!!
    }
}

open class SimpleJsonObject(m: MutableMap<String, SimpleJsonValue<*>?>?) : SimpleJsonValueAdapter<MutableMap<String, SimpleJsonValue<*>?>>(m,
    JsonType.OBJECT
) {
    open var mStrategy: JsonStrategy = JsonStrategy.SIMPLEST
    open var mDepth: Int = 0

    override fun string(): String {
        if (mValue == null) {
            return "null"
        }
        var tempValue: Map<String, SimpleJsonValue<*>?> = mValue!!
        if (tempValue.isEmpty()) {
            return "{}"
        }
        if (!mStrategy.useNull()) {
            tempValue = tempValue.filter { it.value?.value() != null }
            if (tempValue.isNullOrEmpty()) {
                return "{}"
            }
        }
        setRightDepth(mDepth)
        setRightStrategy(mStrategy)
        return if (!mStrategy.useBLANK()) {
            """{${tempValue.map { "\"${it.key}\":${it.value?.string() ?: "null"}" }.joinToString(",")}}"""
        } else {
            val prefix1 = "\t".repeat(mDepth)
            val prefix2 = "$prefix1\t"
            "{\n" + tempValue.map { "${prefix2}\"${it.key}\": ${it.value?.string() ?: "null"}" }.joinToString(",\n") + "\n$prefix1}"
        }
    }

    override fun clone(): SimpleJsonObject {
        val itemMap = mutableMapOf<String, SimpleJsonValue<*>?>()
        mValue?.forEach { itemMap[it.key] = it.value?.clone() }
        val result = SimpleJsonObject(itemMap)
        result.mDepth = mDepth
        result.mStrategy = mStrategy
        return result
    }

    open fun setRightDepth(depth: Int) {
        mDepth = depth
        val nextDepth = depth + 1
        mValue?.forEach {
            val jv = it.value
            if (jv is SimpleJsonArray) {
                jv.setRightDepth(nextDepth)
            }
            if (jv is SimpleJsonObject) {
                jv.setRightDepth(nextDepth)
            }
        }
    }

    open fun setRightStrategy(strategy: JsonStrategy) {
        mStrategy = strategy
        mValue?.forEach {
            val jv = it.value
            if (jv is SimpleJsonArray) {
                jv.setRightStrategy(strategy)
            }
            if (jv is SimpleJsonObject) {
                jv.setRightStrategy(strategy)
            }
        }
    }

    open operator fun get(key: String) = if (mValue.isNullOrEmpty()) null else mValue!![key]
    open operator fun set(key: String, value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.put(key, value) else null
    open operator fun contains(key: String) = if (mValue != null) mValue!!.containsKey(key) else false
    open operator fun contains(value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.containsValue(value) else false
    open operator fun iterator() = mValue?.iterator() ?: EMPTY_OBJECT.iterator()
}

open class SimpleJsonArray(l: MutableList<SimpleJsonValue<*>?>?) : SimpleJsonValueAdapter<MutableList<SimpleJsonValue<*>?>>(l,
    JsonType.ARRAY
) {
    open var mStrategy: JsonStrategy = JsonStrategy.SIMPLEST
    open var mDepth: Int = 0

    init {
        if (mValue != null) {
            val index = mValue!!.indexOfLast { it?.value() != null || it != null && it.type() != JsonType.NUL }
            if (index == -1) {
                mValue = null
            } else if (index != mValue!!.size - 1) {
                mValue = mValue!!.subList(0, index + 1)
            }
        }
    }

    override fun string(): String {
        if (mValue.isNullOrEmpty()) {
            return "null"
        }
        val tempValue = mValue!!
        setRightDepth(mDepth)
        setRightStrategy(mStrategy)
        return if (!mStrategy.useBLANK()) {
            "[${tempValue.joinToString(",") { it?.string() ?: "null" }}]"
        } else {
            val prefix1 = "\t".repeat(mDepth)
            val prefix2 = "$prefix1\t"
            "[\n" + tempValue.joinToString(",\n") { "$prefix2${it?.string() ?: "null"}" } + "\n$prefix1]"
        }
    }

    override fun clone(): SimpleJsonArray {
        val itemList = mutableListOf<SimpleJsonValue<*>?>()
        mValue?.forEach {
            if (it != null) {
                itemList.add(it.clone())
            }
        }
        val result = SimpleJsonArray(itemList)
        result.mDepth = mDepth
        result.mStrategy = mStrategy
        return result
    }

    open fun setRightDepth(depth: Int) {
        mDepth = depth
        val nextDepth = depth + 1
        mValue?.forEach {
            if (it is SimpleJsonArray) {
                it.setRightDepth(nextDepth)
            }
            if (it is SimpleJsonObject) {
                it.setRightDepth(nextDepth)
            }
        }
    }

    open fun setRightStrategy(strategy: JsonStrategy) {
        mStrategy = strategy
        mValue?.forEach {
            if (it is SimpleJsonArray) {
                it.setRightStrategy(strategy)
            }
            if (it is SimpleJsonObject) {
                it.setRightStrategy(strategy)
            }
        }
    }

    open fun add(index: Int, value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.add(index, value) else Unit
    open operator fun plus(value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.add(value) else false
    open operator fun minus(value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.remove(value) else false
    open operator fun minus(index: Int) = if (mValue != null) mValue!!.removeAt(index) else null
    open operator fun get(index: Int) = if (!mValue.isNullOrEmpty()) null else mValue!![index]
    open operator fun get(value: SimpleJsonValue<*>?) = if (!mValue.isNullOrEmpty()) -1 else mValue!!.indexOf(value)
    open operator fun set(index: Int, value: SimpleJsonValue<*>?) = if (mValue != null) mValue!!.set(index, value) else null
    open operator fun iterator() = mValue?.iterator() ?: EMPTY_ARRAY.iterator()
    open operator fun contains(value: SimpleJsonValue<*>?) = if (mValue != null) value in mValue!! else false
}

open class SimpleJsonNumber(n: Number? = null) : SimpleJsonValueAdapter<Number>(n,
    JsonType.NUMBER
) {
    protected var mNumberType: JsonNumberType = JsonNumberType.UNKNOWN

    constructor(b: Byte?) : this(b as? Number) {
        mNumberType = JsonNumberType.BYTE
    }

    constructor(s: Short?) : this(s as? Number) {
        mNumberType = JsonNumberType.SHORT
    }

    constructor(i: Int?) : this(i as? Number) {
        mNumberType = JsonNumberType.INT
    }

    constructor(l: Long?) : this(l as? Number) {
        mNumberType = JsonNumberType.LONG
    }

    constructor(bi: BigInteger?) : this(bi as? Number) {
        mNumberType = JsonNumberType.BIG_INTEGER
    }

    constructor(f: Float?) : this(f as? Number) {
        mNumberType = JsonNumberType.FLOAT
    }

    constructor(d: Double?) : this(d as? Number) {
        mNumberType = JsonNumberType.DOUBLE
    }

    constructor(bd: BigDecimal?) : this(bd as? Number) {
        mNumberType = JsonNumberType.BIG_DECIMAL
    }

    constructor(c: Char?) : this(c?.toLong() as? Number) {
        mNumberType = JsonNumberType.CHAR
    }

    constructor(n: Number?, nt: JsonNumberType) : this(n) {
        mNumberType = nt
    }

    override fun value(): Number? = when {
        mValue == null -> null
        mNumberType == JsonNumberType.CHAR -> mValue!!
        mNumberType == JsonNumberType.UNKNOWN -> mValue!!
        mNumberType == JsonNumberType.BYTE -> mValue!!.toByte()
        mNumberType == JsonNumberType.SHORT -> mValue!!.toShort()
        mNumberType == JsonNumberType.INT -> mValue!!.toInt()
        mNumberType == JsonNumberType.LONG -> mValue!!.toLong()
        mNumberType == JsonNumberType.BIG_INTEGER -> mValue!! as BigInteger
        mNumberType == JsonNumberType.FLOAT -> mValue!!.toFloat()
        mNumberType == JsonNumberType.DOUBLE -> mValue!!.toDouble()
        mNumberType == JsonNumberType.BIG_DECIMAL -> mValue!! as BigDecimal
        else -> null
    }

    override fun string(): String = when {
        mNumberType != JsonNumberType.UNKNOWN -> mValue?.toString() ?: "null"
        else -> "null"
    }

    open fun numberType(): JsonNumberType = mNumberType
    open fun charValue(): Char = (mValue as? Long)?.toChar() ?: '\u0000'

    override fun clone(): SimpleJsonNumber {
        val result = SimpleJsonNumber(mValue)
        result.mNumberType = mNumberType
        return result
    }
}

open class SimpleJsonString(s: String? = null) : SimpleJsonValueAdapter<String>(s,
    JsonType.STRING
) {
    @Suppress("IntroduceWhenSubject")
    override fun string(): String {
        val tempValue = mValue ?: return "null"
        val resultBuilder = StringBuilder("\"")
        var i = 0
        val length = tempValue.length
        while (i < length) {
            val ch = tempValue[i]
            // val chInt = ch.toInt()
            resultBuilder.append(
                when {
                    ch == '\\' -> "\\\\"
                    ch == '"' -> "\\\""
                    ch == '\b' -> "\\b"
                    ch == '\u000C' -> "\\f"
                    ch == '\n' -> "\\n"
                    ch == '\r' -> "\\r"
                    ch == '\t' -> "\\t"
                    // chInt <= 0x1f -> String.format("\\u%04x", chInt) // ???
                    // chInt == 0xe2 && i < length - 2 && tempValue[i + 1].toInt() == 0x80 && tempValue[i + 2].toInt() == 0xa8 -> {
                    //     i += 2
                    //     "\\u2028"
                    // } // ???
                    // chInt == 0xe2 && i < length - 2 && tempValue[i + 1].toInt() == 0x80 && tempValue[i + 2].toInt() == 0xa9 -> {
                    //     i += 2
                    //     "\\u2029"
                    // } // ???
                    else -> ch
                }
            )
            i++
        }
        return resultBuilder.append("\"").toString()
    }

    override fun clone(): SimpleJsonString = SimpleJsonString(mValue)
}

open class SimpleJsonBoolean(b: Boolean? = false) : SimpleJsonValueAdapter<Boolean>(b,
    JsonType.BOOL
) {
    override fun clone(): SimpleJsonBoolean = SimpleJsonBoolean(mValue)
}

open class SimpleJsonNULL : SimpleJsonValueAdapter<Unit>(null, JsonType.NUL) {
    override fun clone(): SimpleJsonNULL = SimpleJsonNULL()
}

// open class SimpleJsonComment(s: String? = null) : SimpleJsonValueAdapter<String>(s, JsonType.COMMENT) {
//     override fun clone(): SimpleJsonValue<String> = SimpleJsonComment(mValue)
// }

val EMPTY_ARRAY = mutableListOf<SimpleJsonValue<*>?>()
val EMPTY_OBJECT = mutableMapOf<String, SimpleJsonValue<*>?>()
val JSON_EMPTY_STRING = SimpleJsonString("")
val JSON_EMPTY_ARRAY = SimpleJsonArray(EMPTY_ARRAY)
val JSON_EMPTY_OBJECT = SimpleJsonObject(EMPTY_OBJECT)
val JSON_TRUE = SimpleJsonBoolean(true)
val JSON_FALSE = SimpleJsonBoolean(false)
val JSON_NULL = SimpleJsonNULL()

open class SimpleJsonParser(var strategy: JsonStyle) {
    open fun parseJson(jsonStr: String): SimpleJsonValue<*> = SimpleJsonParseTask(
        jsonStr,
        strategy
    ).run()
    open fun parseJsonOrThrow(jsonStr: String): SimpleJsonValue<*> = SimpleJsonParseTask(
        jsonStr,
        strategy,
        true
    ).run()
    open fun parseJsonOrNull(jsonStr: String): SimpleJsonValue<*>? = SimpleJsonParseTask(
        jsonStr,
        strategy
    ).runOrNull()

    open class SimpleJsonParseTask(_jsonStr: String, val strategy: JsonStyle, val throwEx: Boolean = false) {
        var jsonStr: String = _jsonStr
            set(value) {
                field = value
                length = value.length
            }
        protected var length = _jsonStr.length
        protected var index: Int = 0
        protected var failReason: String? = null

        fun run(jsonStr: String): SimpleJsonValue<*> {
            this.jsonStr = jsonStr
            index = 0
            failReason = null
            return run()
        }

        fun runOrNull(jsonStr: String): SimpleJsonValue<*>? {
            this.jsonStr = jsonStr
            index = 0
            failReason = null
            return runOrNull()
        }

        fun runOrNull(): SimpleJsonValue<*>? {
            val result = parseJson(0)
            if (index < length) {
                consumeGarbage()
                if (failReason != null) {
                    return null
                }
                if (index < length) {
                    failReason = "invalid format, can't parse end"
                    return null
                }
            }
            if (result == null && failReason == null) {
                failReason = "unknown reason"
            }
            return result
        }

        fun run(): SimpleJsonValue<*> {
            val result = parseJson(0) ?: return SimpleJsonString(failReason ?: "unknown reason")
            if (index < length) {
                consumeGarbage()
                if (failReason != null) {
                    return SimpleJsonString(failReason)
                }
                if (index < length) {
                    val reason = "invalid format, can't parse end"
                    return makeFail<SimpleJsonValue<*>>(reason,
                        SimpleJsonString(reason)
                    ) as SimpleJsonValue<*>
                }
            }
            return result
        }

        protected fun parseNumber(): SimpleJsonNumber? {
            if (index >= length) {
                return makeFail<SimpleJsonNumber>("unexpected start of input in number", null)
            }
            val start = index
            // symbol part
            if (jsonStr[index] == '-') {
                index++
                if (index >= length) {
                    return makeFail<SimpleJsonNumber>("at least one digit required after -", null)
                }
            }
            // integer part
            if (jsonStr[index] == '0') {
                index++
                if (index >= length) {
                    return makeFail<SimpleJsonNumber>("-0 is not a invalid number's present", null)
                }
                if (jsonStr[index] in nonZeroDigitCharRange) {
                    return makeFail<SimpleJsonNumber>("leading 0s not permitted in numbers", null)
                }
            } else if (jsonStr[index] in allDigitCharRange) {
                index++
                while (index < length && jsonStr[index] in allDigitCharRange) {
                    index++
                }
                if (index >= length) {
                    return makeIntegerResult(jsonStr.substring(start))
                }
            } else {
                return makeFail<SimpleJsonNumber>("invalid ${jsonStr[index]} in number", null)
            }
            // dot part
            var ch = jsonStr[index]
            if (ch != '.' && ch != 'e' && ch != 'E') {
                return makeIntegerResult(jsonStr.substring(start, index))
            }
            // decimal part
            if (ch == '.') {
                index++
                if (index >= length || jsonStr[index] !in allDigitCharRange) {
                    return makeFail("at least one digit required in fractional part", null)
                }
                while (index < length && jsonStr[index] in allDigitCharRange) {
                    index++
                }
                if (index >= length) {
                    return makeDecimalResult(jsonStr.substring(start))
                }
            }
            // exponent part
            ch = jsonStr[index]
            if (ch == 'e' || ch == 'E') {
                index++
                if (index >= length) {
                    return makeFail<SimpleJsonNumber>("at least one digit required in exponent part", null)
                }
                ch = jsonStr[index]
                if (ch == '+' || ch == '-') {
                    index++
                    if (index >= length) {
                        return makeFail<SimpleJsonNumber>("at least one digit required in exponent part", null)
                    }
                }
                if (jsonStr[index] !in allDigitCharRange) {
                    return makeFail<SimpleJsonNumber>("at least one digit required in exponent part", null)
                }
                while (index < length && jsonStr[index] in allDigitCharRange) {
                    index++
                }
            }
            return makeDecimalResult(jsonStr.substring(start, index))
        }

        protected fun makeIntegerResult(s: String): SimpleJsonNumber? {
            return if (s[0] == '-' && s.length < 20 || s.length < 19) {
                SimpleJsonNumber(s.toLongOrNull() ?: return makeFail("invalid integer number format", null))
            } else {
                SimpleJsonNumber(s.toBigIntegerOrNull() ?: return makeFail("invalid big integer number format", null))
            }
        }

        protected fun makeDecimalResult(s: String): SimpleJsonNumber? {
            return if (s[0] == '-' && s.length < 20 || s.length < 19) {
                SimpleJsonNumber(s.toDoubleOrNull() ?: return makeFail("invalid decimal number format", null))
            } else {
                SimpleJsonNumber(s.toBigDecimalOrNull() ?: return makeFail("invalid big decimal number format", null))
            }
        }

        protected fun parseString(): SimpleJsonString? {
            return SimpleJsonString(innerParseString() ?: return null)
        }

        protected fun innerParseString(): String? {
            if (index >= length || jsonStr[index] != '"') {
                return makeFail<String>(
                    "unexpected start of input in string: ${if (index >= length) "end" else jsonStr[index].toString()}",
                    null
                )
            }
            index++
            val resultBuilder = StringBuilder()
            while (true) {
                if (index >= length) {
                    return makeFail<String>("unexpected end of input in string", null)
                }
                var ch = jsonStr[index++]
                if (ch == '"') {
                    return resultBuilder.toString()
                }
                if (ch == '\\') {
                    if (index >= length) {
                        return makeFail<String>("required one character after escape symbol", null)
                    }
                    ch = jsonStr[index++]
                    when (ch) {
                        'u' -> {
                            if (index + 4 < length) {
                                try {
                                    ch = jsonStr.substring(index, index + 4).toInt(16).toChar()
                                    index += 4
                                } catch (e: Exception) {
                                }
                            }
                            resultBuilder.append(ch)
                        }
                        'b' -> resultBuilder.append('\b')
                        'f' -> resultBuilder.append('\u000C')
                        'n' -> resultBuilder.append('\n')
                        'r' -> resultBuilder.append('\r')
                        't' -> resultBuilder.append('\t')
                        '"', '\\', '/' -> resultBuilder.append(ch)
                        /*else -> resultBuilder.append('\\') */
                        else -> return makeFail<String>("invalid escape character $ch", null)
                    }
                } else {
                    resultBuilder.append(ch)
                }
            }
        }

        protected fun parseArray(depth: Int): SimpleJsonArray? {
            if (index >= length || jsonStr[index] != '[') {
                return makeFail<SimpleJsonArray>(
                    "unexpected start of input in array: ${if (index >= length) "end" else
                        jsonStr[index].toString()}", null
                )
            }
            index++
            var ch = getNextToken() ?: return makeFail<SimpleJsonArray>("invalid array's present, lack of \"]\"", null)
            val itemList = mutableListOf<SimpleJsonValue<*>?>()
            if (ch != ']') {
                index--
                while (true) {
                    itemList.add(parseJson(depth + 1) ?: return null)
                    ch = getNextToken()
                        ?: return makeFail<SimpleJsonArray>("invalid array's present, lack of \"]\"", null)
                    if (ch == ']') {
                        break
                    }
                    if (ch != ',') {
                        return makeFail<SimpleJsonArray>("expected ',' in list, got '$ch'", null)
                    }
                    consumeGarbage()
                }
            }
            val result = SimpleJsonArray(itemList)
            result.mDepth = depth
            return result
        }

        protected fun parseObject(depth: Int): SimpleJsonObject? {
            if (index >= length || jsonStr[index] != '{') {
                return makeFail<SimpleJsonObject>(
                    "unexpected start of input in object: ${if (index >= length) "end" else
                        jsonStr[index].toString()}", null
                )
            }
            index++
            var ch = getNextToken()
                ?: return makeFail<SimpleJsonObject>("invalid object's present, lack of \"}\"", null)
            val itemMap = mutableMapOf<String, SimpleJsonValue<*>?>()
            if (ch != '}') {
                index--
                while (true) {
                    val key = innerParseString() ?: return null
                    ch = getNextToken()
                        ?: return makeFail<SimpleJsonObject>("invalid object's present, lack of \"}\"", null)
                    if (ch != ':') {
                        return makeFail<SimpleJsonObject>("expected ':' in object, got '$ch'", null)
                    }
                    itemMap[key] = (parseJson(depth + 1) ?: return null)
                    ch = getNextToken()
                        ?: return makeFail<SimpleJsonObject>("invalid object's present, lack of \"}\"", null)
                    if (ch == '}') {
                        break
                    }
                    if (ch != ',') {
                        return makeFail<SimpleJsonObject>("expected ',' in object, got '$ch'", null)
                    }
                    consumeGarbage()
                }
            }
            val result = SimpleJsonObject(itemMap)
            result.mDepth = depth
            return result
        }

        protected fun parseJson(depth: Int): SimpleJsonValue<*>? {
            val ch = getNextToken() ?: makeFail<SimpleJsonValue<*>>("json format is incorrect, the json string become empty before end parsing", null)
            index--
            return when (ch) {
                '-', in allDigitCharRange -> parseNumber()
                't' -> expectJsonValue("true", JSON_TRUE)
                'f' -> expectJsonValue("false", JSON_FALSE)
                'n' -> expectJsonValue("null", JSON_NULL)
                '"' -> parseString()
                '{' -> parseObject(depth + 1)
                '[' -> parseArray(depth + 1)
                else -> makeFail("expected value, got $ch", null)
            }
        }

        protected fun expectJsonValue(expected: String, result: SimpleJsonValue<*>): SimpleJsonValue<*>? {
            if (length - index < expected.length) {
                index = length
                return makeFail<SimpleJsonValue<*>>("parse error -- expected: $expected, got: ${jsonStr.substring(index)}", null)
            }
            val start = index
            expected.forEach {
                if (it != jsonStr[index++]) {
                    return makeFail<SimpleJsonValue<*>>(
                        "parse error -- expected: $expected, got: " + jsonStr.substring(
                            start, start
                                    + expected.length
                        ), null
                    )
                }
            }
            return result
        }

        protected fun getNextToken(): Char? {
            consumeGarbage()
            return if (index >= length) makeFail<Char>("unexpected end of input", null) else jsonStr[index++]
        }

        protected fun consumeGarbage() {
            consumeWhiteSpaces()
            if (strategy == JsonStyle.COMMENTS) {
                var commentFound: Boolean
                do {
                    commentFound = consumeComments()
                    if (failReason != null) {
                        return
                    }
                    consumeWhiteSpaces()
                } while (commentFound)
            }
        }

        protected fun consumeComments(): Boolean {
            if (index < length && jsonStr[index] == '/') {
                index++
                if (index >= length) {
                    return makeFail("unexpected end of input after start of comment", false) as Boolean
                }
                when {
                    jsonStr[index] == '/' -> {
                        index++
                        while (index < length && jsonStr[index] != '\n') {
                            index++
                        }
                    }
                    jsonStr[index] == '*' -> {
                        index++
                        if (index > length - 2) {
                            return makeFail("unexpected end of input inside multi-line comment", false) as Boolean
                        }
                        var indexPlusOne = index + 1
                        while (!(jsonStr[index] == '*' && jsonStr[indexPlusOne] == '/')) {
                            index = indexPlusOne
                            indexPlusOne++
                            if (indexPlusOne == length) {
                                index = length
                                return makeFail("unexpected end of input inside multi-line comment", false) as Boolean
                            }
                        }
                        index += 2
                    }
                    else -> return makeFail("malformed comment", false) as Boolean
                }
                return true
            }
            return false
        }

        protected fun consumeWhiteSpaces() {
            while (index < length && (jsonStr[index] == ' ' || jsonStr[index] == '\r' || jsonStr[index] == '\n' || jsonStr[index] == '\t')) {
                index++
            }
        }

        protected fun <T> makeFail(reason: String, result: T?): T? {
            failReason = reason
            if (throwEx) {
                throw RuntimeException("failReason: $failReason, index: $index, length: $length, less: ${if (index < length) jsonStr.substring(index) else "none"}")
            }
            return result
        }
    }

    companion object {
        fun fromJson(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonValue<*> = if (strategy == JsonStyle.STANDARD) {
            standardJsonParser.parseJson(jsonStr)
        } else {
            commentsJsonParser.parseJson(jsonStr)
        }

        fun fromJsonOrThrow(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonValue<*> = if (strategy == JsonStyle.STANDARD) {
            standardJsonParser.parseJsonOrThrow(jsonStr)
        } else {
            commentsJsonParser.parseJsonOrThrow(jsonStr)
        }

        fun fromJsonOrNull(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonValue<*>? = if (strategy == JsonStyle.STANDARD) {
            standardJsonParser.parseJsonOrNull(jsonStr)
        } else {
            commentsJsonParser.parseJsonOrNull(jsonStr)
        }

        val allDigitCharRange = '0'..'9'
        val nonZeroDigitCharRange = '1'..'9'
    }
}

val standardJsonParser = SimpleJsonParser(JsonStyle.STANDARD)
val commentsJsonParser = SimpleJsonParser(JsonStyle.COMMENTS)

open class SimpleJsonApi {
    protected var jsonValue: SimpleJsonValue<*> = JSON_NULL

    constructor()
    constructor(obj: Boolean) : this(if (obj) JSON_TRUE else JSON_FALSE)
    constructor(obj: Byte) : this(SimpleJsonNumber(obj))
    constructor(obj: Short) : this(SimpleJsonNumber(obj))
    constructor(obj: Int) : this(SimpleJsonNumber(obj))
    constructor(obj: Long) : this(SimpleJsonNumber(obj))
    constructor(obj: BigInteger) : this(SimpleJsonNumber(obj))
    constructor(obj: Float) : this(SimpleJsonNumber(obj))
    constructor(obj: Double) : this(SimpleJsonNumber(obj))
    constructor(obj: BigDecimal) : this(SimpleJsonNumber(obj))
    constructor(obj: Char) : this(SimpleJsonNumber(obj))
    constructor(obj: String) : this(if (obj.isEmpty()) JSON_EMPTY_STRING else SimpleJsonString(
        obj
    )
    )
    constructor(obj: MutableList<SimpleJsonValue<*>?>) : this(if (obj.isEmpty()) JSON_EMPTY_ARRAY else SimpleJsonArray(
        obj
    )
    )
    constructor(obj: MutableMap<String, SimpleJsonValue<*>?>) : this(if (obj.isEmpty()) JSON_EMPTY_OBJECT else SimpleJsonObject(
        obj
    )
    )

    constructor(obj: SimpleJsonValue<*>) {
        jsonValue = obj
    }

    open fun type(): JsonType = jsonValue.type()
    open fun value(): Any? = jsonValue.value()
    open fun jsonValue(): SimpleJsonValue<*> = jsonValue
    open fun string(): String = jsonValue.string()

    companion object {
        fun fromJson(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonApi =
            SimpleJsonApi(
                SimpleJsonParser.fromJson(
                    jsonStr,
                    strategy
                )
            )

        fun fromJsonOrThrow(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonApi =
            SimpleJsonApi(
                SimpleJsonParser.fromJsonOrThrow(
                    jsonStr,
                    strategy
                )
            )

        fun fromJsonOrNull(jsonStr: String, strategy: JsonStyle = JsonStyle.STANDARD): SimpleJsonApi? {
            return SimpleJsonApi(
                SimpleJsonParser.fromJsonOrNull(
                    jsonStr,
                    strategy
                ) ?: return null
            )
        }
    }
}

// TODO: SimpleJsonParseTask2(val stream: InputStream)
