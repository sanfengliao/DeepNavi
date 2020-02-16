package com.sysu.deepnavi.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.widget.Toast

/*[Android Camera2 之 CameraManager 详解](https://blog.csdn.net/afei__/article/details/85342160)
void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler)
void unregisterAvailabilityCallback(AvailabilityCallback callback)
void registerTorchCallback(TorchCallback callback, Handler handler)
void unregisterTorchCallback(TorchCallback callback)
void setTorchMode(String cameraId, boolean enabled)*/

open class CameraUtil2(
    open val activity: Activity,
    open val previewView: View,
    open val pictureSize: Size = Size(1080, 1920),
    open val previewCallback: (data: ByteArray, imageReader: ImageReader, width: Int, height: Int) -> Unit
) {
    companion object {
        const val TAG = "CameraUtil2"

        val ORIENTATIONS = SparseIntArray()

        // 为了使照片竖直显示
        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    open val backgroundHandlerThread = HandlerThread("cameraUtil2")
    open val backgroundHandler: Handler
    open val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    open var cameraId: String? = null
    open var sensorOrientation: Int = 0
    open var cameraCharacteristics: CameraCharacteristics? = null
    open var cameraDevice: CameraDevice? = null
    open var cameraLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

    open var imageReader: ImageReader? = null
    open var previewRequestBuilder: CaptureRequest.Builder? = null
    open var captureSession: CameraCaptureSession? = null

    open val cameraAvailabilityCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            if (cameraId != cameraId) {
                return
            }
            if (checkCameraPermission(activity)) {
                Log.d(TAG, "onCameraAvailable -- failed, because there are not enough permissions")
                return
            }
            openCamera()
        }
    }
    open val captureSessionCaptureCallback = object : CameraCaptureSession.CaptureCallback() {}
    open val captureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            captureSession = cameraCaptureSession
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "Fail while starting preview: ")
        }
    }
    open val imageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            val imageReader = reader ?: return
            val image = imageReader.acquireNextImage()
            val buffer = image.planes[0].buffer
            var bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            if (sensorOrientation != 0) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                val matrix = Matrix()
                matrix.postRotate(sensorOrientation.toFloat())
                bytes = bitmapToByteArray(Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true))
            }
            previewCallback(bytes, imageReader, imageReader.width, imageReader.height)
        }
    }
    open val cameraDeviceStateCallback: CameraDevice.StateCallback

    init {
        requestCameraPermissions(activity)
        selectCameraId()
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
        cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
            private var surface: Surface? = null

            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                val workingSurface = when (val mView = previewView) {
                    is SurfaceView -> mView.holder.surface
                    is TextureView -> {
                        surface = Surface(mView.surfaceTexture)
                        surface!!
                    }
                    else -> return
                } ?: return
                previewRequestBuilder.addTarget(workingSurface)

                val streamConfigurationMap = cameraCharacteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        as StreamConfigurationMap
                val outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)
                // val largesImageSize = Collections.max(outputSizes.toMutableList()) { lhs, rhs ->
                //     (lhs!!.width.toLong() * lhs.height - rhs!!.width.toLong() * rhs.height).toInt()
                // }
                // imageReader = ImageReader.newInstance(largesImageSize.width, largesImageSize.height, ImageFormat.JPEG, 3)
                val exchange = exchangeWidthAndHeight(activity.windowManager.defaultDisplay.rotation, sensorOrientation)
                val judgePictureSize = selectPictureSize(outputSizes.toList(), when {
                    exchange -> pictureSize
                    else -> Size(pictureSize.height, pictureSize.width)
                }
                )
                if (judgePictureSize == null) {
                    Log.e(TAG, "judgeOneCamera -- camera($cameraId) doesn't supported any picture size")
                    return
                } else {
                    val suitablePictureSize = judgePictureSize.first
                    imageReader = ImageReader.newInstance(suitablePictureSize.height, suitablePictureSize.width, ImageFormat.JPEG, 3)
                    imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
                    previewRequestBuilder.addTarget(imageReader!!.surface)
                }
                previewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                this@CameraUtil2.previewRequestBuilder = previewRequestBuilder

                camera.createCaptureSession(
                    when {
                        imageReader != null -> listOf(workingSurface, imageReader!!.surface)
                        else -> listOf(workingSurface)
                    }, captureSessionStateCallback, backgroundHandler
                )
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                surface?.release()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Toast.makeText(activity, "cameraManager.StateCallback.onError: cameraId: ${camera.id}, error: $error", Toast.LENGTH_LONG).show()
                camera.close()
                surface?.release()
            }
        }

        if (previewView is SurfaceView) {
            (previewView as SurfaceView).holder.run {
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
                        openCamera()
                    }
                })
            }
        } else if (previewView is TextureView) {
            (previewView as TextureView).surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
                    openCamera()
                    startPreview()
                }
            }
        }
    }

    open fun onResume() = cameraManager.registerAvailabilityCallback(cameraAvailabilityCallback, backgroundHandler)
    open fun onPause() = cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback)

    /**
     * 根据提供的屏幕方向 [displayRotation] 和相机方向 [sensorOrientation] 返回是否需要交换宽高
     */
    private fun exchangeWidthAndHeight(displayRotation: Int, sensorOrientation: Int): Boolean {
        var exchange = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    exchange = true
                }
            Surface.ROTATION_90, Surface.ROTATION_270 ->
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    exchange = true
                }
            else -> Log.d(TAG, "Display rotation is invalid: $displayRotation")
        }
        Log.d(TAG, "屏幕方向  $displayRotation, 相机方向  $sensorOrientation")
        return exchange
    }

    open fun selectCameraId() {
        if (checkCameraPermission(activity)) {
            Log.d(TAG, "selectCameraId -- failed, because there are not enough permissions")
            return
        }
        val ids: Array<String> = cameraManager.cameraIdList
        for (id in ids) {
            val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val orientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
            if (orientation == CameraCharacteristics.LENS_FACING_BACK || orientation == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                if (cameraId == null || level > cameraLevel) {
                    val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue
                    if (!streamConfigurationMap.isOutputSupportedFor(ImageFormat.JPEG)) {
                        return
                    }
                    val supportedSizes = streamConfigurationMap.getOutputSizes(previewView::class.java)
                        ?: streamConfigurationMap.getOutputSizes(ImageFormat.JPEG) ?: continue
                    selectPreviewSize(supportedSizes.toList(), previewView.width, previewView.height)?.first ?: continue
                    cameraId = id
                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                    cameraCharacteristics = characteristics
                    cameraLevel = level
                }
            }
        }
    }

    open fun openCamera() {
        if (checkCameraPermission(activity)) {
            Log.d(TAG, "openCamera -- failed, because there are not enough permissions")
            return
        }
        if (cameraDevice != null) {
            Log.d(TAG, "openCamera -- failed, because the camera have opened")
            return
        }
        try {
            if (cameraId == null) {
                selectCameraId()
            }
            cameraManager.openCamera(cameraId ?: return, cameraDeviceStateCallback, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error during camera initialize")
        }
    }

    open fun startPreview() {
        captureSession?.setRepeatingRequest(previewRequestBuilder?.build() ?: return, captureSessionCaptureCallback, backgroundHandler)
    }

    open fun stopPreview() {
        captureSession?.stopRepeating()
    }

    open fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
    }
}

// [Android平台Camera开发实践指南](https://juejin.im/post/5a33a5106fb9a04525782db5)
// [Android Camera2 教程 · 第二章 · 开关相机](https://www.jianshu.com/p/df3c8683bb90)
