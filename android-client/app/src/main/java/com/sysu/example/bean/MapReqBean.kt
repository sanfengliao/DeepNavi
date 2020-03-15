package com.sysu.example.bean

val INVALID_FLOAT_ARRAY = floatArrayOf()
val INVALID_ADJACENCE = arrayOf<String>()
const val INVALID_STR = ""

open class GetPath {
    open var mapId: String? = null
    open var src: Fuck = Fuck()
    open var dst: Fuck = Fuck()

    open class Fuck {
        open var actualCoordinate = INVALID_COORDINATOR
    }
}
