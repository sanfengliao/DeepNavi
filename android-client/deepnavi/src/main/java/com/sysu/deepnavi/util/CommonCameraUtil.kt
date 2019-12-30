package com.sysu.deepnavi.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.min

const val TAG = "CommonCameraUtil"
const val PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE = 1

/**
 * 将Camera api的Size转变为通用的Size
 * @param sizes camera.size
 */
fun toUtilSize(sizes: List<Camera.Size?>?): List<Size> = if (sizes.isNullOrEmpty()) listOf()
else sizes.mapNotNull { if (it == null) null else Size(it.width, it.height) }

/**
 * 选择最佳预览size，先是根据宽高比来获取最接近的四个，然后选择面积最接近的那个
 * @param sizes 支持的预览size
 * @param width surfaceView或者textureView的宽度(竖屏时)
 * @param height surfaceView或者textureView的高度(竖屏时)
 * @return 返回由选择出来的size和是否符合预期的布尔值组成
 */
fun selectPreviewSize(sizes: List<Size?>?, width: Int, height: Int, methodName: String = "selectPreviewSize"): Pair<Size, Boolean>? {
    val msg = "$methodName -- height: $height, width: $width, sizes: " +
            (sizes?.joinToString { if (it == null) "null" else "(${it.width}, ${it.height})" } ?: "null")
    if (sizes.isNullOrEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val notNullSizes = sizes.filterNotNull()
    if (notNullSizes.isEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val rate: Float = height.toFloat() / width  // 宽高比
    val area: Int = height * width  // 面积
    val rateSizes = notNullSizes.map {
        val tempRate = it.width.toFloat() / it.height
        if (tempRate == rate && it.width == height && it.height == width) {
            return Pair(it, true)
        }
        Pair(abs(tempRate - rate), it)
    }.sortedBy { it.first }  // 根据宽高比来比较
    val result = rateSizes.subList(0, min(4, rateSizes.size)).map {
        val size = it.second
        Pair(abs(size.width * size.height - area), size)
    }.minBy { it.first }!!.second
    Log.d(TAG, "$msg, result -- ${"(${result.width}, ${result.height})"}")
    return Pair(result, false)
}

/**
 * 选择最佳拍照size，目前采取和选择最佳预览size一样的策略
 * @param sizes 支持的拍照size
 * @param targetSize 目标size
 * @return 返回由size和是否符合预期的布尔值组成
 */
fun selectPictureSize(sizes: List<Size?>?, targetSize: Size): Pair<Size, Boolean>? =
    selectPreviewSize(sizes, targetSize.width, targetSize.height, "selectPictureSize")

/**
 * 选择最佳预览帧率，选择和目标帧率最接近的那个
 * @param frameRates 支持的帧率
 * @param targetFrameRate 目标帧率
 * @return 返回由选择出来的帧率和是否符合预期的布尔值组成
 */
fun selectPreviewFrameRate(frameRates: List<Int?>?, targetFrameRate: Int): Pair<Int, Boolean>? {
    val msg = "selectPreviewFrameRate -- targetFrameRate: $targetFrameRate, frameRates: ${frameRates?.joinToString() ?: "null"}"
    if (frameRates.isNullOrEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val notNullFrameRates = frameRates.filterNotNull()
    if (notNullFrameRates.isEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val result = notNullFrameRates.map {
        if (it == targetFrameRate) {
            return Pair(targetFrameRate, true)
        }
        Pair(abs(targetFrameRate - it), it)
    }.minBy { it.first }!!.second
    Log.d(TAG, "$msg, result -- $result")
    return Pair(result, false)
}

/**
 * 选择最佳预览帧率范围，选择和目标帧率最接近的那个，先是选择包含targetFps的，然后选择范围最小的四个，最后选择两边离它差不多的。如果没有包含它的，那就选择某个离它最近的
 * @param fpsRanges 支持的帧率范围
 * @param targetFps 目标帧率
 * @return 返回由选择出来的帧率范围和是否符合预期的布尔值组成
 * TODO: test
 */
fun selectPreviewFpsRange(fpsRanges: List<IntArray?>?, targetFps: Int): Pair<IntArray, Boolean>? {
    val msg = "selectPreviewFpsRange -- targetFps: $targetFps, fpsRange: " +
            (fpsRanges?.joinToString { it?.joinToString() ?: "null" } ?: "null")
    if (fpsRanges.isNullOrEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val notNullFpsRanges = fpsRanges.filterNotNull()
    if (notNullFpsRanges.isEmpty()) {
        Log.d(TAG, "$msg, result -- null")
        return null
    }
    val containsFpsRange = notNullFpsRanges.filter { it[0] <= targetFps && targetFps <= it[1] }  // 包含
    if (containsFpsRange.isEmpty()) {
        val notContainsFpsRange = notNullFpsRanges.map {
            Pair(min(abs(it[0] - targetFps), abs(it[1] - targetFps)), it)
        }.sortedBy { it.first }  // 如果没有包含的就选择离targetFps最近的
        var equalSize = 1
        while (equalSize < notContainsFpsRange.size && notContainsFpsRange[equalSize].first == notContainsFpsRange[0].first) {
            equalSize++
        }
        val result = if (equalSize == 1) {
            notContainsFpsRange[0].second
        } else {
            notContainsFpsRange.subList(0, equalSize).map {
                val range = it.second
                Pair(range[1] - range[0], range)
            }.minBy { it.first }!!.second  // 根据范围排序
        }
        Log.d(TAG, "$msg, result -- $result")
        return Pair(result, false)
    }
    val sortedByAreaFpsRange = containsFpsRange.map { Pair(it[1] - it[0], it) }.sortedBy { it.first }  // 根据范围排序
    val result = sortedByAreaFpsRange.subList(0, min(4, sortedByAreaFpsRange.size)).map {
        val range = it.second
        Pair(abs(range[1] + range[0] - targetFps * 2), range)
    }.minBy { it.first }!!.second
    Log.d(TAG, "$msg, result -- $result")
    return Pair(result, true)
}

/**
 * 设置 摄像头的角度
 * @param activity 上下文
 * @param cameraId 摄像头ID（假如手机有N个摄像头，cameraId 的值 就是 0 ~ N-1）
 * @param camera   摄像头对象
 */
fun getCameraDisplayOrientation(activity: Activity, cameraId: Int): Int {
    val info = Camera.CameraInfo()
    // 获取摄像头信息
    Camera.getCameraInfo(cameraId, info)
    // 获取摄像头当前的角度
    var degrees = 0
    when (activity.windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_0 -> degrees = 0
        Surface.ROTATION_90 -> degrees = 90
        Surface.ROTATION_180 -> degrees = 180
        Surface.ROTATION_270 -> degrees = 270
    }
    val result: Int = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // 前置摄像头
        (360 - (info.orientation + degrees) % 360) % 360 // compensate the mirror
    } else { // back-facing  后置摄像头
        (info.orientation - degrees + 360) % 360
    }
    Log.d(TAG, "camera.setDisplayOrientation($result)")
    return result
}

fun rotateYUV420Degree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
    // prepare
    val temp = imageWidth * imageHeight
    val size = temp * 3 / 2
    val yuv = ByteArray(size)
    // Rotate the Y luma
    var i = 0
    (0 until imageWidth).forEach { x ->
        ((imageHeight - 1) downTo 0).forEach { y ->
            yuv[i++] = data[y * imageWidth + x]
        }
    }
    // Rotate the U and V color components
    i = size - 1
    IntProgression.fromClosedRange(imageWidth - 1, 0, 2).forEach { x ->
        (0 until imageHeight.shr(1)).forEach { y ->
            val temp2 = temp + (y * imageWidth) + x
            yuv[i--] = data[temp2]
            yuv[i--] = data[temp2 - 1]
        }
    }
    // result
    return yuv
}

fun rotateYUV420Degree180(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
    // prepare
    val temp = imageWidth * imageHeight
    val size = temp * 3 / 2
    val yuv = ByteArray(size)
    // transform
    var count = 0
    ((temp - 1) downTo 0).forEach { i ->
        yuv[count++] = data[i]
    }
    IntProgression.fromClosedRange(size - 1, temp, 2).forEach { i ->
        yuv[count++] = data[i - 1]
        yuv[count++] = data[i]
    }
    // result
    return yuv
}

fun rotateYUV420Degree270(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
    // prepare
    val temp = imageWidth * imageHeight
    val size = temp * 3 / 2
    val yuv = ByteArray(size)
    val uvHeight = imageHeight.shl(1)
    // transform
    var k = 0
    (0 until imageWidth).forEach { i ->
        var nPos = 0
        (0 until imageHeight).forEach { j ->
            yuv[k++] = data[nPos + i]
            nPos += imageWidth
        }
    }
    IntProgression.fromClosedRange(0, imageWidth - 1, 2).forEach { i ->
        var nPos = temp
        (0 until uvHeight).forEach { j ->
            yuv[k] = data[nPos + i]
            yuv[k + 1] = data[nPos + i + 1]
            k += 2
            nPos += imageWidth
        }
    }
    // result
    return rotateYUV420Degree180(yuv, imageWidth, imageHeight)
}

/**
 * 获取 相机权限
 * @param activity 开启权限必要的activity
 */
fun requestCameraPermissions(activity: Activity) {
    if (checkCameraPermission(activity)) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ),
            PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE
        )
        Log.d(TAG, "requestPermissions")
    }
}

/**
 * 检查相机权限
 * @param context 检查权限必要的context
 */
fun checkCameraPermission(context: Context): Boolean = Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
        (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
