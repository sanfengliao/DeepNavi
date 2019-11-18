package com.sysu.deepnavi.impl

import android.app.Activity
import android.hardware.Camera
import android.view.View
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.CameraUtil1
import java.util.*

class AudioListener(activity: Activity, view: View, private val frameRate: Int = 50, autoFocus: Boolean = true) : DataCollectorInter<ByteArray> {
    override val field: String = "image"
    private var data: ByteArray? = null
    private val dataList: LinkedList<ByteArray> = LinkedList()
    private var cameraUtil: CameraUtil1 = CameraUtil1(activity, view, Camera.PreviewCallback { data, _ ->
        this.data = data
        synchronized(dataList) {
            dataList.addFirst(data)
            if (dataList.size > frameRate) dataList.removeFirst()
        }
    }, frameRate, autoFocus)

    override fun getData(): ByteArray? = data

    override fun getDataArray(): Array<ByteArray>? {
        synchronized(dataList) {
            return dataList.toArray() as Array<ByteArray>
        }
    }
}
