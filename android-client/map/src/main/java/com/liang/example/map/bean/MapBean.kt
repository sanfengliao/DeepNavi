package com.liang.example.map.bean

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val INVALID_COORDINATOR = DeepNaviCoordinator()
val INVALID_DEEPNAVI_POINT = DeepNaviPoint()

/**
 * import numpy as np
 * def coordinateRotation(roll: float, pitch: float, yaw: float, coor: np.matrix) -> np.matrix:
 *     roll = np.pi * roll / 180
 *     pitch = np.pi * pitch / 180
 *     yaw = np.pi * yaw / 180
 *     return np.matrix([
 *         [np.cos(yaw) * np.cos(pitch), np.sin(yaw) * np.sin(pitch) * np.sin(roll) - np.sin(yaw) * np.cos(roll), np.sin(yaw) * np.sin(roll) + np.cos(yaw) * np.cos(roll) * np.sin(pitch)],
 *         [np.cos(pitch) * np.sin(yaw), np.cos(yaw) * np.cos(roll) + np.sin(yaw) * np.sin(roll) * np.sin(pitch), np.cos(roll) * np.sin(yaw) * np.sin(pitch) - np.cos(yaw) * np.sin(roll)],
 *         [-np.sin(pitch), np.cos(pitch) * np.sin(roll), np.cos(roll) * np.cos(pitch)]
 *     ]) * coor
 *
 * @param roll 坐标系绕x轴旋转的角度
 * @param pitch 坐标系绕y轴旋转的角度
 * @param yaw 坐标系绕z轴旋转的角度
 * @param coordinate 坐标(必须是x,y,z这样的)
 */
fun rotateCoordinate(roll: Float, pitch: Float, yaw: Float, coordinate: List<Float>): List<Float> {
    val r = roll * Math.PI.toFloat() / 180
    val p = pitch * Math.PI.toFloat() / 180
    val y = yaw * Math.PI.toFloat() / 180
    val cr = cos(r)
    val sr = sin(r)
    val cp = cos(p)
    val sp = sin(p)
    val cy = cos(y)
    val sy = sin(y)
    val temp = matrixMultipleF(
        listOf(
            listOf(cy * cp, sy * sp * sr - sy * cr, sy * sr + cy * cr * sp),
            listOf(cp * sy, cy * cr + sy * sr * sp, cr * sy * sp - cy * sr),
            listOf(-sp, cp * sr, cr * cp)
        ),
        listOf(listOf(coordinate[0]), listOf(coordinate[1]), listOf(coordinate[2]))
    )!!
    return listOf(temp[0][0], temp[1][0], temp[2][0])
}

/**
 * 简单的矩阵乘法
 */
fun matrixMultipleF(m1: List<List<Float>>, m2: List<List<Float>>): List<List<Float>>? =
    if (m1.isNotEmpty() && m2.isNotEmpty() && m1[0].size == m2.size) {
        val len3 = m2[0].size
        m1.map { list1 ->
            (0 until len3).map { list2Index ->
                list1.mapIndexed { index, item -> item * m2[index][list2Index] }.reduce { acc, i -> acc + i }
            }
        }
    } else {
        null
    }

/**
 * 向量夹角公式
 */
fun angleBetweenVector(x1: Float, y1: Float, x2: Float, y2: Float): Float =
    x1 * x2 + y1 * y2 / sqrt(x1.pow(2) + y1.pow(2)) * sqrt(x2.pow(2) + y2.pow(2))

/**
 * def quaternion_to_euler(rotation_output: torch.Tensor) -> typing.List:
 *     rotation_w = rotation_output[0]
 *     rotation_x = rotation_output[1]
 *     rotation_y = rotation_output[2]
 *     rotation_z = rotation_output[3]
 *     x = math.atan2(2 * (rotation_y * rotation_z + rotation_w * rotation_x),
 *                    (rotation_w * rotation_w - rotation_x * rotation_x - rotation_y * rotation_y + rotation_z * rotation_z))
 *     y = math.asin(2 * (rotation_w * rotation_y - rotation_x * rotation_z))
 *     z = math.atan2(2 * (rotation_x * rotation_y + rotation_w * rotation_z),
 *                    (rotation_w * rotation_w + rotation_x * rotation_x - rotation_y * rotation_y - rotation_z * rotation_z))
 *     return [x * 180 / math.pi, y * 180 / math.pi, z * 180 / math.pi]
 */
fun quaternionToEuler(q: List<Float>): List<Float> {
    val w = q[0]
    val x = q[1]
    val y = q[2]
    val z = q[3]
    val x2 = atan2(2 * (y * z + w * x), (w * w - x * x - y * y + z * z))
    val y2 = asin(2 * (w * y - x * z))
    val z2 = atan2(2 * (x * y + w * z), (w * w + x * x - y * y - z * z))
    return listOf(x2 * 180 / Math.PI.toFloat(), y2 * 180 / Math.PI.toFloat(), z2 * 180 / Math.PI.toFloat())
}

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
    open var isClockWise: Boolean = false
    open var standardVector = "0,1"
    open var offsetAngle = 0f

    open var planPath: String? = null
    open var modelPath: String? = null
    open var id: String? = null

    // 从模型坐标系到手机坐标系
    open fun modelToWorld(coordinate: List<Float>, actual: Boolean = false): List<Float> {
        val temp = rotateCoordinate(rotationAngle[0], rotationAngle[1], rotationAngle[2], coordinate)
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
        return rotateCoordinate(
            -rotationAngle[0], -rotationAngle[1], -rotationAngle[2],
            listOf(coordinate[0] - temp2[0], coordinate[1] - temp2[1], coordinate[2] - temp2[2])
        )
    }

    // 从手机坐标系到模型坐标系
    open fun worldToModel(coordinate: DeepNaviCoordinator, actual: Boolean = false): DeepNaviCoordinator =
        DeepNaviCoordinator(worldToModel(listOf(coordinate.x, coordinate.y, coordinate.z), actual))

    open fun percentagePoint(x: Float, y: Float, z: Float = 0f): DeepNaviPoint {
        val point = DeepNaviPoint()
        if (id != null) {
            point.mapId = id!!
        }
        point.planCoordinate = DeepNaviCoordinator(x * planSize[0], y * planSize[1], z * planSize[2])
        point.actualCoordinate = DeepNaviCoordinator(x * actualSize[0], y * actualSize[1], z * actualSize[2])
        return point
    }

    open fun actualPoint(x: Float, y: Float, z: Float = 0f): DeepNaviPoint {
        val point = DeepNaviPoint()
        if (id != null) {
            point.mapId = id!!
        }
        point.planCoordinate = DeepNaviCoordinator(x * planSize[0] / actualSize[0], y * planSize[1] / actualSize[1], z * planSize[2] / actualSize[2])
        point.actualCoordinate = DeepNaviCoordinator(x, y, z)
        return point
    }

    open fun planPoint(x: Float, y: Float, z: Float = 0f): DeepNaviPoint {
        val point = DeepNaviPoint()
        if (id != null) {
            point.mapId = id!!
        }
        point.planCoordinate = DeepNaviCoordinator(x, y, z)
        point.actualCoordinate = DeepNaviCoordinator(x * actualSize[0] / planSize[0], y * actualSize[1] / planSize[1], z * actualSize[2] / planSize[2])
        return point
    }
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
