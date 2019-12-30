package com.sysu.deepnavi.util

import android.app.Activity
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.Size
import android.view.*

/*[Android 摄像头预览](https://www.jianshu.com/p/cf55f42f0cb7)
[关于Android使用Camera自定义拍照出现模糊不清的解决方案](https://blog.csdn.net/u012228009/article/details/43450609)
[一句代码搞定权限请求，从未如此简单](https://www.jianshu.com/p/c69ff8a445ed)
[Android 6.0 运行时权限处理](https://www.jianshu.com/p/b4a8b3d4f587)
[玩转Android Camera开发(二):使用TextureView和SurfaceTexture预览Camera 基础拍照demo](https://blog.csdn.net/yanzi1225627/article/details/33313707)
[Android 使Camera预览清晰，循环自动对焦处理](https://blog.csdn.net/z979451341/article/details/79446025)*/

@Deprecated(message = "Please use CameraUtil2")
open class CameraUtil1(
    private val activity: Activity,
    private val previewView: View,
    private val previewCallback: Camera.PreviewCallback? = null,
    private val frameRate: Int = 50,
    private val pictureSize: Size = Size(1080, 1920),
    autoFocus: Boolean = true
) {
    companion object {
        const val TAG = "CameraUtil1"
        const val MSG_AUTO_FOCUS = 2
        const val AUTO_FOCUS_INTERVAL = 1000L  // 自动对焦时间

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
        requestCameraPermissions(activity)
        if (previewView is SurfaceView) {
            previewView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val width = previewView.measuredWidth
            val height = previewView.measuredHeight
            previewView.holder.run {
                setFormat(PixelFormat.TRANSPARENT)
                addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                        Log.d(TAG, "SurfaceHolder -- surfaceChanged -- format: $format, width: $width, height: $height")
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder?) {
                        Log.d(TAG, "SurfaceHolder -- surfaceDestroyed")
                        stopPreview()
                        closeCamera()
                    }

                    override fun surfaceCreated(holder: SurfaceHolder?) {
                        Log.d(TAG, "SurfaceHolder -- surfaceCreated")
                        openCamera(width, height)
                        startPreview()
                    }
                })
            }
        } else if (previewView is TextureView) {
            previewView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureSizeChanged -- width: $width, height: $height")
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                    // Log.d(TAG, "SurfaceTexture -- onSurfaceTextureUpdated")
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureDestroyed")
                    stopPreview()
                    closeCamera()
                    return true
                }

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceTexture -- onSurfaceTextureAvailable -- width: $width, height: $height")
                    openCamera(width, height)
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

    fun openCamera(width: Int, height: Int) {
        if (checkCameraPermission(activity)) {
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
        val defaultDisplay = activity.windowManager.defaultDisplay
        // val judgeCameraList = mutableListOf<Triple<Camera.Parameters, Int, Int>>()
        for (id in 0 until cameraCount) {
            Camera.getCameraInfo(id, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                continue
            }
            backFlag = false
            try {
                camera = Camera.open(id)
            } catch (e: RuntimeException) {
                Log.e(TAG, "createCamera -- open camera($id) failed", e)
                closeCamera()
                continue
            }

            val cameraParameters = camera!!.parameters
            val judgeCamera = judgeOneCamera(cameraParameters, width, height, id, defaultDisplay)
            if (judgeCamera.third != 4) {  // 如果想改变策略，可以将所有的
                closeCamera()
                continue
            }
            // judgeCameraList.add(judgeCamera)

            try {
                camera!!.parameters = cameraParameters
            } catch (e: RuntimeException) {
                Log.e(TAG, "createCamera -- set parameters for camera($id) is failed", e)
                continue
            }
            setCameraDisplayOrientation(activity, id, camera!!)
            break
        }
        Log.d(
            TAG, when {
                backFlag -> "createCamera -- failed, because this phone does not have a back camera"
                camera == null -> "createCamera -- failed, because cameras are disabled"
                else -> "createCamera -- successfully"
            }
        )
    }

    protected fun judgeOneCamera(cameraParameters: Camera.Parameters, width: Int, height: Int, id: Int, defaultDisplay: Display)
            : Triple<Camera.Parameters, Int, Int> {
        // prepare
        var suitableCount = 0
        var setCount = 0
        val supportedPreviewSizes = toUtilSize(cameraParameters.supportedPreviewSizes)
        val supportedPictureSizes = toUtilSize(cameraParameters.supportedPictureSizes)
        val supportedVideoSizes = toUtilSize(cameraParameters.supportedVideoSizes)
        val supportedJpegThumbnailSizes = toUtilSize(cameraParameters.supportedJpegThumbnailSizes)
        val supportedPreviewFrameRates = cameraParameters.supportedPreviewFrameRates
        val supportedPreviewFpsRange = cameraParameters.supportedPreviewFpsRange
        val supportedAntibanding = cameraParameters.supportedAntibanding
        val supportedColorEffects = cameraParameters.supportedColorEffects
        val supportedFlashModes = cameraParameters.supportedFlashModes
        val supportedFocusModes = cameraParameters.supportedFocusModes
        val supportedPreviewFormats = cameraParameters.supportedPreviewFormats
        val supportedSceneModes = cameraParameters.supportedSceneModes
        val supportedWhiteBalance = cameraParameters.supportedWhiteBalance

        Log.d(
            TAG,
            "createCamera -- camera($id), supportedPreviewSizes: " + supportedPreviewSizes.joinToString { "(${it.width}, ${it.height})" } +
                    "\nsupportedVideoSizes: " + supportedVideoSizes.joinToString { "(${it.width}, ${it.height})" } +
                    "\nsupportedPictureSizes: " + supportedPictureSizes.joinToString { "(${it.width}, ${it.height})" } +
                    "\nsupportedJpegThumbnailSizes: " + supportedJpegThumbnailSizes.joinToString { "(${it.width}, ${it.height})" } +
                    "\ndefaultDisplay.width: ${defaultDisplay.width}, defaultDisplay.height: ${defaultDisplay.height}" +
                    "\nsupportedPreviewFrameRates: ${supportedPreviewFrameRates.joinToString()}" +
                    "\nsupportedPreviewFpsRange: ${supportedPreviewFpsRange.joinToString { """[${it.joinToString("~")}]""" }}" +
                    "\nsupportedAntibanding: ${supportedAntibanding.joinToString()}" +
                    "\nsupportedColorEffects: ${supportedColorEffects.joinToString()}" +
                    "\nsupportedFlashModes: ${supportedFlashModes.joinToString()}" +
                    "\nsupportedFocusModes: ${supportedFocusModes.joinToString()}" +
                    "\nsupportedPreviewFormats: ${supportedPreviewFormats.joinToString()}" +
                    "\nsupportedSceneModes: ${supportedSceneModes.joinToString()}" +
                    "\nsupportedWhiteBalance: ${supportedWhiteBalance.joinToString()}"
        )

        // 选择预览size
        val judgePreViewSize = selectPreviewSize(supportedPreviewSizes, width, height)
        if (judgePreViewSize == null) {
            Log.e(TAG, "judgeOneCamera -- camera($id) doesn't supported any preview size")
        } else {
            if (judgePreViewSize.second) {
                suitableCount++
            }
            val suitablePreviewSize = judgePreViewSize.first
            cameraParameters.setPreviewSize(suitablePreviewSize.width, suitablePreviewSize.height)
            Log.d(TAG, "judgeOneCamera -- camera($id) setPreviewSize(${suitablePreviewSize.width}, ${suitablePreviewSize.height})")
            setCount++
        }

        // 选择拍照size
        val judgePictureSize = selectPictureSize(supportedPictureSizes, pictureSize)
        if (judgePictureSize == null) {
            Log.e(TAG, "judgeOneCamera -- camera($id) doesn't supported any picture size")
        } else {
            if (judgePictureSize.second) {
                suitableCount++
            }
            val suitablePictureSize = judgePictureSize.first
            cameraParameters.setPictureSize(suitablePictureSize.width, suitablePictureSize.height)
            Log.d(TAG, "judgeOneCamera -- camera($id) setPictureSize(${suitablePictureSize.width}, ${suitablePictureSize.height})")
            setCount++
        }

        // 选择预览帧率
        val judgePreviewFrameRate = selectPreviewFrameRate(cameraParameters.supportedPreviewFrameRates, frameRate)
        if (judgePreviewFrameRate == null) {
            Log.e(TAG, "judgeOneCamera -- camera($id) doesn't supported any frame rate")
        } else {
            if (judgePreviewFrameRate.second) {
                suitableCount++
            }
            cameraParameters.previewFrameRate = judgePreviewFrameRate.first
            Log.d(TAG, "judgeOneCamera -- camera($id) previewFrameRate = ${judgePreviewFrameRate.first}")
            setCount++
        }

        // 选择预览帧率范围
        val judgePreviewFpsRange = selectPreviewFpsRange(cameraParameters.supportedPreviewFpsRange, frameRate)
        if (judgePreviewFpsRange == null) {
            Log.e(TAG, "judgeOneCamera -- camera($id) doesn't supported any fps range")
        } else {
            if (judgePreviewFpsRange.second) {
                suitableCount++
            }
            val suitableFpsRange = judgePreviewFpsRange.first
            cameraParameters.setPreviewFpsRange(suitableFpsRange[0], suitableFpsRange[1])
            Log.d(TAG, "judgeOneCamera -- camera($id) setPreviewFpsRange(${suitableFpsRange[0]}, ${suitableFpsRange[1]})")
            setCount++
        }

        cameraParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

        return Triple(cameraParameters, suitableCount, setCount)
    }

    open fun startPreview() {
        if (checkCameraPermission(activity)) {
            Log.d(TAG, "startPreview -- failed, because there are not enough permissions")
            return
        }
        camera?.run {
            when (previewView) {
                is SurfaceView -> setPreviewDisplay(previewView.holder)
                is TextureView -> setPreviewTexture(previewView.surfaceTexture)
                else -> throw java.lang.RuntimeException("cameraUtil1 only support surfaceView or textureView")
            }
            setPreviewCallback(previewCallback)
            startPreview()
            if (autoFocusCallback != null) {
                autoFocusHandler?.camera = this
                autoFocus(autoFocusCallback)
            }
            Log.d(TAG, "startPreview -- successfully")
        } ?: Log.d(TAG, "startPreview -- failed, because the camera is null")
    }

    open fun stopPreview() {
        autoFocusHandler?.removeCallbacksAndMessages(null)
        camera?.setPreviewCallback(null)
        camera?.stopPreview()
    }

    open fun closeCamera() {
        camera?.release()
        camera = null
    }
}
