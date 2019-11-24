package com.sysu.deepnavi.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import androidx.core.content.ContextCompat


class CameraUtil2(private val activity: Activity, private val view: View, private val frameRate: Int = 50, private val background: Boolean = true) {
    companion object {
        const val TAG = "CameraUtil2"
    }

    private var cameraManager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val comparator = Comparator<Size> { s1, s2 ->
        if (s1.height == s2.height) s2.width - s1.width else s2.height - s1.height
    }
    private val defaultDisplay = activity.windowManager.defaultDisplay
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val id = cameraDevice!!.id
            Log.d(TAG, "openCamera -- CameraDevice.StateCallback -- onOpened")
            val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map != null) {
                val pictureSizeList = map.getOutputSizes(ImageFormat.JPEG)
                val previewSizeList = map.getOutputSizes(view.javaClass)
                val videoSizeList = map.getOutputSizes(MediaRecorder::class.java)
                pictureSizeList.sortWith(comparator)
                previewSizeList.sortWith(comparator)
                videoSizeList.sortWith(comparator)
                Log.d(
                    TAG,
                    "createCamera -- cameraId: $id, pictureSizeList: " + pictureSizeList.joinToString { "(${it.width}, ${it.height})" } +
                            "\npreviewSizeList: " + previewSizeList.joinToString { "(${it.width}, ${it.height})" } +
                            "\nvideoSizeList: " + videoSizeList.joinToString { "(${it.width}, ${it.height})" } +
                            "\ndefaultDisplay.width: ${defaultDisplay.width}, defaultDisplay.height: ${defaultDisplay.height}")
                val builder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            }
            Log.d(TAG, "openCamera -- Successfully! id is $id")
        }

        override fun onClosed(camera: CameraDevice) {
            Log.d(TAG, "openCamera -- CameraDevice.StateCallback -- onClosed")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "openCamera -- CameraDevice.StateCallback -- onError: $error")
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "openCamera -- CameraDevice.StateCallback -- onDisconnected")
        }
    }
    private var cameraDevice: CameraDevice? = null
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null

    init {
        CameraUtil1.requestPermissions(activity)
        // manager = activity.getSystemService(CameraManager::class.java) as CameraManager  // minApi -- 23
    }

    private fun startBackgroundThread() {
        handlerThread = HandlerThread("CameraBackground")
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
        Log.d(TAG, "startBackgroundThread")
    }

    private fun stopBackgroundThread() {
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            Log.d(TAG, "stopBackgroundThread", e)
        }
    }


    fun openCamera(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
            (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        ) {
            Log.d(TAG, "openCamera -- failed, because there are not enough permissions")
            return false
        }
        if (handler == null && !background) handler = Handler(Looper.getMainLooper())
        else if (handler == null && background) startBackgroundThread()

        val ids: Array<String> = cameraManager.cameraIdList
        var backFlag = false
        for (id in ids) {
            val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val internal = characteristics.get(CameraCharacteristics.LENS_FACING) ?: return false
            if (internal == CameraCharacteristics.LENS_FACING_BACK) {
                backFlag = true
                try {
                    cameraManager.openCamera(id, stateCallback, handler)
                } catch (e: CameraAccessException) {
                    Log.w(TAG, "openCamera -- Failed to open camera: $id", e)
                    continue
                }
                return true
            }
        }
        Log.d(
            TAG, "openCamera -- Failed to open camera, " + (if (!backFlag) "because this phone does not have a back camera"
            else "because there are no available cameras or cameras are disabled")
        )
        return false
    }

    fun closeCamera() {
    }

    // [Android Camera2 之 CameraManager 详解](https://blog.csdn.net/afei__/article/details/85342160)
    // void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler)
    // void unregisterAvailabilityCallback(AvailabilityCallback callback)
    // void registerTorchCallback(TorchCallback callback, Handler handler)
    // void unregisterTorchCallback(TorchCallback callback)
    // void setTorchMode(String cameraId, boolean enabled)
}