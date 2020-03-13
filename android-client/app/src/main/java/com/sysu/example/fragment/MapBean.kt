package com.sysu.example.fragment

val INVALID_COORDINATOR = DeepNaviCoordinator()
val INVALID_DEEPNAVI_POINT = DeepNaviPoint()

open class DeepNaviCoordinator {
    open var x = -1f
    open var y = -1f
    open var z = -1f

    constructor()
    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }
}

open class DeepNaviMap {
    open var id = INVALID_STR
    open var name = INVALID_STR
    open var planPath = INVALID_STR
    open var planSize = INVALID_FLOAT_ARRAY
    open var planUnit = "px"
    open var actualSize = INVALID_FLOAT_ARRAY
    open var actualUnit = "m"
    open var originInPlan = INVALID_FLOAT_ARRAY
    open var modelPath = INVALID_STR
}

open class DeepNaviPoint {
    open var id = INVALID_STR
    open var mapId = INVALID_STR
    open var name = INVALID_STR
    // open var planCoordinate = INVALID_FLOAT_ARRAY
    // open var actualCoordinate = INVALID_FLOAT_ARRAY
    open var planCoordinate = INVALID_COORDINATOR
    open var actualCoordinate = INVALID_COORDINATOR
    open var adjacence = INVALID_ADJACENCE
}

open class DeepNaviEdge {
    open var mapId = INVALID_STR
    open var startPointId = INVALID_STR
    open var endPointId = INVALID_STR
}
