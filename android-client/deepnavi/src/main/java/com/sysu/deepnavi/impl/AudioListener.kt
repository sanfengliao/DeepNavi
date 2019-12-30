package com.sysu.deepnavi.impl

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.util.Size
import android.view.View
import com.google.protobuf.ByteString
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.CameraUtil1
import com.sysu.deepnavi.util.rotateYUV420Degree180
import com.sysu.deepnavi.util.rotateYUV420Degree270
import com.sysu.deepnavi.util.rotateYUV420Degree90
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class AudioListener(
    activity: Activity,
    view: View,
    private val frameRate: Int = 50,
    pictureSize: Size = Size(1080, 1920),
    autoFocus: Boolean = true
) : DataCollectorInter<ByteString> {
    override val field: String = "image"
    private var data: ByteString? = null
    private val dataList: LinkedList<ByteString> = LinkedList()
    private var cameraUtil: CameraUtil1 = CameraUtil1(activity, view, Camera.PreviewCallback { data, camera ->
        // [Android：如何将预览帧保存为jpeg图像？](http://cn.voidcc.com/question/p-wxlhpyrt-ec.html)
        // [android PreviewCallback方法中获取图片](https://blog.csdn.net/getchance/article/details/51537434)
        // TODO: 多线程
        val parameters = camera.parameters
        val size = parameters.previewSize

        // 角度调整
        val displayOrientation = getDisplayOrientation()
        val newData: ByteArray
        newData = when (displayOrientation) {
            90 -> rotateYUV420Degree90(data, size.width, size.height)
            180 -> rotateYUV420Degree180(data, size.width, size.height)
            270 -> rotateYUV420Degree270(data, size.width, size.height)
            else -> data
        }

        val image = YuvImage(newData, ImageFormat.NV21, size.width, size.height, null)
        val rectangle = Rect(0, 0, size.width, size.height)
        val out = ByteArrayOutputStream()
        image.compressToJpeg(rectangle, 100, out)
        val jpegData = out.toByteArray()

        // val options = BitmapFactory.Options()
        // options.inSampleSize = 1
        // val bmp = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size);
        // val bmp = BitmapFactory.decodeStream(ByteArrayInputStream(jpegData), null, options)

        this.data = ByteString.copyFrom(jpegData)
        synchronized(dataList) {
            dataList.addFirst(this.data)
            if (dataList.size > frameRate) dataList.removeFirst()
            null
        }
    }, frameRate, pictureSize, autoFocus)

    override fun getData(): ByteString? = data

    private fun getDisplayOrientation() = cameraUtil.getDisplayOrientation()

    @Suppress("UNCHECKED_CAST")
    override fun getDataArray(): Array<ByteString>? {
        synchronized(dataList) {
            return dataList.toArray() as Array<ByteString>
        }
    }
}
