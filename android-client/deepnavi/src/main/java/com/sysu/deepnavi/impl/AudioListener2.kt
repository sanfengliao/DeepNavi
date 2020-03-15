package com.sysu.deepnavi.impl

import android.app.Activity
import android.util.Size
import android.view.View
import com.google.protobuf.ByteString
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.CameraUtil2
import com.sysu.deepnavi.util.bitmapToByteArray
import com.sysu.deepnavi.util.createScaledBitmap
import java.util.LinkedList

open class AudioListener2(
    activity: Activity,
    view: View?,
    private val frameRate: Int = 50,
    private val pictureSize: Size = Size(1080, 1920),
    autoFocus: Boolean = true
) : DataCollectorInter<ByteString> {
    override val field: String = "image"

    private var data: ByteString? = null
    private val dataList: LinkedList<ByteString> = LinkedList()

    val cameraUtil = CameraUtil2(activity, view, pictureSize) { data, _, width, height ->
        var jpegData = data
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
    val haveOpenCamera: Boolean
        get() = cameraUtil.cameraDevice != null

    override fun getData(): ByteString? = data
    @Suppress("UNCHECKED_CAST")
    override fun getDataArray(): Array<ByteString>? = synchronized(dataList) {
        return dataList.toArray() as Array<ByteString>
    }
}
