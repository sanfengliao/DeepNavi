package com.sysu.example.bean

import com.sysu.example.activity.MapActivity

val INVALID_COORDINATOR = DeepNaviCoordinator()
val INVALID_DEEPNAVI_POINT = DeepNaviPoint()

open class DeepNaviCoordinator {
    open var x = -1f
    open var y = -1f
    open var z = -1f

    constructor()
    constructor(coordinator: List<Float>) {
        this.x = coordinator[0]
        this.y = coordinator[1]
        this.z = coordinator[2]
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }
}

open class DeepNaviMap {
    open var name = INVALID_STR
    open var planUnit = "px"
    open var actualUnit = "m"
    open var planSize = INVALID_FLOAT_ARRAY
    open var actualSize = INVALID_FLOAT_ARRAY
    open var originInPlan = INVALID_FLOAT_ARRAY
    open var originInActual = INVALID_FLOAT_ARRAY
    open var rotationAngle = INVALID_FLOAT_ARRAY

    open var planPath: String? = null
    open var modelPath: String? = null
    open var id: String? = null

    // 从模型坐标系到手机坐标系
    open fun modelToWorld(coordinate: List<Float>, actual: Boolean = false): List<Float> {
        val temp = MapActivity.rotateCoordinate(rotationAngle[0], rotationAngle[1], rotationAngle[2], coordinate)
        val temp2 = when (actual) {
            true -> originInActual
            else -> originInPlan
        }
        return listOf(temp[0] + temp2[0], temp[1] + temp2[1], temp[2] + temp2[2])
    }

    // 从模型坐标系到手机坐标系
    open fun modelToWorld(coordinate: DeepNaviCoordinator, actual: Boolean = false): DeepNaviCoordinator =
        DeepNaviCoordinator(modelToWorld(listOf(coordinate.x, coordinate.y, coordinate.z), actual))

    // 从手机坐标系到模型坐标系
    open fun worldToModel(coordinate: List<Float>, actual: Boolean = false): List<Float> {
        val temp2 = when (actual) {
            true -> originInActual
            else -> originInPlan
        }
        return MapActivity.rotateCoordinate(
            -rotationAngle[0], -rotationAngle[1], -rotationAngle[2],
            listOf(coordinate[0] - temp2[0], coordinate[1] - temp2[1], coordinate[2] - temp2[2])
        )
    }

    // 从手机坐标系到模型坐标系
    open fun worldToModel(coordinate: DeepNaviCoordinator, actual: Boolean = false): DeepNaviCoordinator =
        DeepNaviCoordinator(worldToModel(listOf(coordinate.x, coordinate.y, coordinate.z), actual))
}

/**
 * 规定发送到server的是modelMode，接受server的也是modelMode，但是本地要用worldMode，所以发送时使用 worldToModel ，接受时使用 modelToWorld
 */
open class DeepNaviPoint {
    open var id: String? = null
    open var mapId = INVALID_STR
    open var name: String? = null
    // open var worldMode: Boolean = true

    // open var planCoordinate = INVALID_FLOAT_ARRAY
    // open var actualCoordinate = INVALID_FLOAT_ARRAY
    open var planCoordinate = INVALID_COORDINATOR
    open var actualCoordinate = INVALID_COORDINATOR
    open var adjacence = INVALID_ADJACENCE

    open fun modelToWorld(mapInfo: DeepNaviMap) {
        // if (worldMode) {
        //     return
        // }
        // worldMode = true
        this.planCoordinate = mapInfo.modelToWorld(this.planCoordinate)
        this.actualCoordinate = mapInfo.modelToWorld(this.actualCoordinate, true)
    }

    open fun worldToModel(mapInfo: DeepNaviMap) {
        // if (!worldMode) {
        //     return
        // }
        // worldMode = false
        this.planCoordinate = mapInfo.worldToModel(this.planCoordinate)
        this.actualCoordinate = mapInfo.worldToModel(this.actualCoordinate, true)
    }
}

open class DeepNaviEdge {
    open var mapId = INVALID_STR
    open var startPointId = INVALID_STR
    open var endPointId = INVALID_STR

    constructor()
    constructor(mapId: String, spi: String, epi: String) {
        this.mapId = mapId
        this.startPointId = spi
        this.endPointId = epi
    }
}
