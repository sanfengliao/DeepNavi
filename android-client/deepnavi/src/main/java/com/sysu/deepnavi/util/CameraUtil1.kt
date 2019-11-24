package com.sysu.deepnavi.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.min

// [Android 摄像头预览](https://www.jianshu.com/p/cf55f42f0cb7)
// [关于Android使用Camera自定义拍照出现模糊不清的解决方案](https://blog.csdn.net/u012228009/article/details/43450609)
// [一句代码搞定权限请求，从未如此简单](https://www.jianshu.com/p/c69ff8a445ed)
// [Android 6.0 运行时权限处理](https://www.jianshu.com/p/b4a8b3d4f587)
// [玩转Android Camera开发(二):使用TextureView和SurfaceTexture预览Camera 基础拍照demo](https://blog.csdn.net/yanzi1225627/article/details/33313707)
// [Android 使Camera预览清晰，循环自动对焦处理](https://blog.csdn.net/z979451341/article/details/79446025)

@Deprecated(message = "Please use CameraUtil2")
class CameraUtil1(
    private val activity: Activity,
    private val view: View,
    private val previewCallback: Camera.PreviewCallback? = null,
    private val frameRate: Int = 50,
    autoFocus: Boolean = true
) {
    companion object {
        const val TAG = "CameraUtil1"
        const val PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE = 1
        const val MSG_AUTO_FOCUS = 2
        const val AUTO_FOCUS_INTERVAL = 1000L  // 自动对焦时间

        /**
         * 设置 摄像头的角度
         *
         * @param activity 上下文
         * @param cameraId 摄像头ID（假如手机有N个摄像头，cameraId 的值 就是 0 ~ N-1）
         * @param camera   摄像头对象
         */
        fun setCameraDisplayOrientation(
            activity: Activity, cameraId: Int, camera: Camera
        ) {
            val info = Camera.CameraInfo()
            // 获取摄像头信息
            Camera.getCameraInfo(cameraId, info)
            val rotation = activity.windowManager.defaultDisplay.rotation
            // 获取摄像头当前的角度
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            var result: Int
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { // 前置摄像头
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360 // compensate the mirror
            } else { // back-facing  后置摄像头
                result = (info.orientation - degrees + 360) % 360
            }
            Log.d(TAG, "camera.setDisplayOrientation($result)")
            camera.setDisplayOrientation(result)
        }

        class AutoFocusHandler(private val autoFocusCallback: Camera.AutoFocusCallback) : Handler() {
            var camera: Camera? = null
            override fun handleMessage(msg: Message) {
                // Log.d(TAG, "AutoFocusHandler -- handleMessage: ${msg.what}")
                when (msg.what) {
                    MSG_AUTO_FOCUS -> camera?.autoFocus(autoFocusCallback) ?: Log.d(TAG, "AutoFocusHandler -- handleMessage: camera is null")
                }
            }
        }
    }

    class MyAutoFocusCallback : Camera.AutoFocusCallback {
        private var mAutoFocusHandler: Handler? = null
        private var mAutoFocusMessage: Int = 0

        fun setHandler(autoFocusHandler: Handler, autoFocusMessage: Int) {
            this.mAutoFocusHandler = autoFocusHandler
            this.mAutoFocusMessage = autoFocusMessage
        }

        override fun onAutoFocus(success: Boolean, camera: Camera?) {
            if (mAutoFocusHandler != null) {
                mAutoFocusHandler!!.sendEmptyMessageDelayed(mAutoFocusMessage, AUTO_FOCUS_INTERVAL)
                // Log.d(TAG, "MyAutoFocusCallback.onAutoFocus -- successfully")
            } else Log.d(TAG, "MyAutoFocusCallback.onAutoFocus -- mAutoFocusHandler isn't null")
        }
    }

    private var camera: Camera? = null
    private var autoFocusHandler: AutoFocusHandler? = null
    private var autoFocusCallback: MyAutoFocusCallback? = null

    init {
        requestPermissions()
        if (view is SurfaceView) {
            view.holder.run {
                setFormat(PixelFormat.TRANSPARENT)
                addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                        Log.d(TAG, "SurfaceHolder -- surfaceChanged -- format: $format, width: $width, height: $height")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder?) {
                        Log.d(TAG, "SurfaceHolder -- surfaceDestroyed")
                        stopPreview()
                        releaseCamera()
                    }

                    override fun surfaceCreated(holder: SurfaceHolder?) {
                        Log.d(TAG, "SurfaceHolder -- surfaceCreated")
                        createCamera()
                        startPreview()
                    }
                })
            }
        } else if (view is TextureView) {
            view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureSizeChanged -- width: $width, height: $height")
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                    // Log.d(TAG, "SurfaceTexture -- onSurfaceTextureUpdated")
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureDestroyed")
                    stopPreview()
                    releaseCamera()
                    return true
                }

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureAvailable -- width: $width, height: $height")
                    createCamera()
                    startPreview()
                }
            }
        }
        if (autoFocus) {
            autoFocusCallback = MyAutoFocusCallback()
            autoFocusHandler = AutoFocusHandler(autoFocusCallback!!)
            autoFocusCallback!!.setHandler(autoFocusHandler!!, MSG_AUTO_FOCUS)
        }
    }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
            (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        ) {
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

    fun createCamera() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
            (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(TAG, "createCamera -- failed, because there are not enough permissions")
            return
        }
        val cameraCount = Camera.getNumberOfCameras()
        if (cameraCount == 0) {
            Log.d(TAG, "createCamera -- failed, because this phone has no camera")
            return
        }
        val cameraInfo: Camera.CameraInfo = Camera.CameraInfo()
        var backFlag = true
        val comparator = Comparator<Camera.Size> { s1, s2 ->
            if (s1.height == s2.height) s2.width - s1.width else s2.height - s1.height
        }
        for (i in 0 until cameraCount) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backFlag = false
                try {
                    camera = Camera.open(i)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "createCamera -- open camera($i) failed", e)
                    continue
                }
                val cameraParameters = camera!!.parameters
                val defaultDisplay = activity.windowManager.defaultDisplay
                val supportedPreviewSizes = cameraParameters.supportedPreviewSizes
                val supportedVideoSizes = cameraParameters.supportedVideoSizes
                val supportedPictureSizes = cameraParameters.supportedPictureSizes
                val supportedJpegThumbnailSizes = cameraParameters.supportedJpegThumbnailSizes
                supportedPreviewSizes.sortWith(comparator)
                supportedVideoSizes.sortWith(comparator)
                supportedPictureSizes.sortWith(comparator)
                supportedJpegThumbnailSizes.sortWith(comparator)
                Log.d(
                    TAG,
                    "createCamera -- cameraId: $i, supportedPreviewSizes: " + supportedPreviewSizes.joinToString { "(${it.width}, ${it.height})" } +
                            "\nsupportedVideoSizes: " + supportedVideoSizes.joinToString { "(${it.width}, ${it.height})" } +
                            "\nsupportedPictureSizes: " + supportedPictureSizes.joinToString { "(${it.width}, ${it.height})" } +
                            "\nsupportedJpegThumbnailSizes: " + supportedJpegThumbnailSizes.joinToString { "(${it.width}, ${it.height})" } +
                            "\ndefaultDisplay.width: ${defaultDisplay.width}, defaultDisplay.height: ${defaultDisplay.height}")
                var it = supportedPreviewSizes[0]
                cameraParameters.setPreviewSize(it.width, it.height)
                Log.d(TAG, "createCamera -- cameraId: $i, setPreviewSize(${it.width}, ${it.height})")
                it = supportedPictureSizes[0]
                cameraParameters.setPictureSize(it.width, it.height)
                Log.d(TAG, "createCamera -- cameraId: $i, setPictureSize(${it.width}, ${it.height})")

                val supportedPreviewFrameRates = cameraParameters.supportedPreviewFrameRates
                val supportedPreviewFpsRange = cameraParameters.supportedPreviewFpsRange
                supportedPreviewFrameRates.sort()
                Log.d(
                    TAG, "createCamera -- cameraId: $i, supportedPreviewFrameRates: ${supportedPreviewFrameRates.joinToString()}" +
                            "\nsupportedPreviewFpsRange: ${supportedPreviewFpsRange.joinToString { """[${it.joinToString("~")}]""" }}"
                )
                val suitableFrameRate = supportedPreviewFrameRates.mapIndexed { j, v -> Pair(abs(v - this.frameRate), j) }.minBy { it.first }
                if (suitableFrameRate != null) {
                    cameraParameters.previewFrameRate = supportedPreviewFrameRates[suitableFrameRate.second]
                    Log.d(TAG, "createCamera -- cameraId: $i, setPreviewFrameRate: ${cameraParameters.previewFrameRate}")
                } else {
                    Log.e(TAG, "createCamera -- cameraId: $i, doesn't support any frameRate")
                    continue
                }
                val targetFps = cameraParameters.previewFrameRate * 1000
                var suitableFpsRange = supportedPreviewFpsRange.find { it[0] < targetFps && targetFps < it[1] }
                if (suitableFpsRange != null) {
                    cameraParameters.setPreviewFpsRange(suitableFpsRange[0], suitableFpsRange[1])
                    Log.d(TAG, "createCamera -- cameraId: $i, setPreviewFpsRange: [${suitableFpsRange[0]}, ${suitableFpsRange[1]}]")
                } else {
                    val suitableFpsRangeTuple =
                        supportedPreviewFpsRange.mapIndexed { j, v -> Pair(min(abs(v[0] - targetFps), abs(v[1] - targetFps)), j) }.minBy { it.first }
                    if (suitableFpsRangeTuple != null) {
                        suitableFpsRange = supportedPreviewFpsRange[suitableFpsRangeTuple.second]
                        cameraParameters.setPreviewFpsRange(suitableFpsRange[0], suitableFpsRange[1])
                        Log.d(TAG, "createCamera -- cameraId: $i, setPreviewFpsRange: [${suitableFpsRange[0]}, ${suitableFpsRange[1]}]")
                    } else {
                        Log.d(TAG, "createCamera -- cameraId: $i, doesn't have suitableFpsRange")
                        continue
                    }
                }

                // val supportedPreviewFormats = cameraParameters.supportedPreviewFormats
                // val supportedPictureFormats = cameraParameters.supportedPictureFormats
                cameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

                try {
                    camera!!.parameters = cameraParameters
                } catch (e: RuntimeException) {
                    Log.e(TAG, "createCamera -- set parameters for camera($i) is failed", e)
                    continue
                }
                setCameraDisplayOrientation(activity, i, camera!!)
                break
            }
        }
        Log.d(
            TAG, when {
                backFlag -> "createCamera -- failed, because this phone does not have a back camera"
                camera == null -> "createCamera -- failed, because cameras are disabled"
                else -> "createCamera -- successfully"
            }
        )
    }

    fun startPreview() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
            (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(TAG, "startPreview -- failed, because there are not enough permissions")
            return
        }
        camera?.run {
            if (view is SurfaceView) setPreviewDisplay(view.holder) else if (view is TextureView) setPreviewTexture(view.surfaceTexture)
            setPreviewCallback(previewCallback)
            startPreview()
            if (autoFocusCallback != null) {
                autoFocusHandler?.camera = this
                autoFocus(autoFocusCallback)
            }
            Log.d(TAG, "startPreview -- successfully")
        } ?: Log.d(TAG, "startPreview -- failed, because the camera is null")
    }

    fun stopPreview() {
        autoFocusHandler?.removeCallbacksAndMessages(null)
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
    }

    fun releaseCamera() {
        camera?.release()
        camera = null
    }
}
