@file:Suppress("DEPRECATION")

package com.sysu.deepnavi.impl

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Size
import android.view.View
import com.google.protobuf.ByteString
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.CameraUtil1
import com.sysu.deepnavi.util.MutableSize
import com.sysu.deepnavi.util.bitmapToByteArray
import com.sysu.deepnavi.util.createScaledBitmap
import java.io.ByteArrayOutputStream
import java.util.LinkedList

class ImageListener(
    activity: Activity,
    view: View?,
    private val frameRate: Int = 50,
    private val pictureSize: MutableSize = MutableSize(1080, 1920),
    autoFocus: Boolean = true
) : DataCollectorInter<ByteString> {
    companion object {
        const val WHAT_HANDLE_IMAGE_DATA = 1234
    }

    override val field: String = "image"
    private var data: ByteString? = null
    private val dataList: LinkedList<ByteString> = LinkedList()

    private val handlerThread: HandlerThread = HandlerThread("camera-api-1-handler")
    private val handler: Handler

    init {
        handlerThread.start()
        handler = object : Handler(handlerThread.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    WHAT_HANDLE_IMAGE_DATA -> prepareData(msg.arg1, msg.arg2, msg.obj as ByteArray)
                }
            }
        }
    }

    val cameraUtil: CameraUtil1 = CameraUtil1(activity, view, { data, camera, width, height ->
        // [Android：如何将预览帧保存为jpeg图像？](http://cn.voidcc.com/question/p-wxlhpyrt-ec.html)
        // [android PreviewCallback方法中获取图片](https://blog.csdn.net/getchance/article/details/51537434)
        handler.sendMessage(handler.obtainMessage(WHAT_HANDLE_IMAGE_DATA, width, height, data))
    }, frameRate, pictureSize, autoFocus)
    val haveOpenCamera: Boolean
        get() = cameraUtil.camera != null

    private fun prepareData(width: Int, height: Int, data: ByteArray) {
        val image = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        image.compressToJpeg(Rect(0, 0, width, height), 100, out)
        var jpegData = out.toByteArray()

        if (width != pictureSize.width || height != pictureSize.height) {
            jpegData = bitmapToByteArray(createScaledBitmap(jpegData, pictureSize.width, pictureSize.height))
        }

        this.data = ByteString.copyFrom(jpegData)
        synchronized(dataList) {
            dataList.addFirst(this.data)
            if (dataList.size > frameRate) {
                dataList.removeFirst()
            }
            null
        }
    }

    override fun getData(): ByteString? = data
    @Suppress("UNCHECKED_CAST")
    override fun getDataArray(): Array<ByteString>? = synchronized(dataList) {
        return dataList.toArray() as Array<ByteString>
    }
}
