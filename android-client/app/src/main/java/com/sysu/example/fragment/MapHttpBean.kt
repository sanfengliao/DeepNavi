package com.sysu.example.fragment

val INVALID_FLOAT_ARRAY = floatArrayOf()
val INVALID_ADJACENCE = arrayOf<String>()
const val INVALID_STR = ""

open class UploadMap {
    open var name = ""
    open var planUnit = "px"
    open var actualUnit = "m"
    open var planSize = INVALID_FLOAT_ARRAY
    open var actualSize = INVALID_FLOAT_ARRAY
    open var originInPlan = INVALID_FLOAT_ARRAY
    open var originInActual = INVALID_FLOAT_ARRAY

    fun toDeepNaviMap(): DeepNaviMap {
        val result = DeepNaviMap()
        result.name = name
        result.planUnit = planUnit
        result.actualUnit = actualUnit
        result.planSize = planSize
        result.actualSize = actualSize
        result.originInPlan = originInPlan
        return result
    }
}

open class AddPoint {
    open var mapId = ""
    open var name: String? = null
    open var planCoordinate = INVALID_COORDINATOR
    open var actualCoordinate = INVALID_COORDINATOR
}

open class AddEdge {
    open var mapId = ""
    open var pointAId: String? = null
    open var pointBId: String? = null
}
