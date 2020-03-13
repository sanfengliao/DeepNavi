@file:Suppress("unused")

package com.sysu.deepnavi

// [几种常用的传感器（加速度传感器、重力传感器、方向传感器、陀螺仪）简介](https://blog.csdn.net/LEON1741/article/details/77200220) finished
// [Android 传感器开发详解](https://blog.csdn.net/Airsaid/article/details/52902299) finished
// [Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](https://blog.csdn.net/flowingflying/article/details/43233315)
// [地磁传感器 Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](http://www.manew.com/blog-166329-34441.html)
// [Pro Android学习笔记](https://blog.csdn.net/flowingflying/article/details/6212512)
// [Android wifi属性简介 及 wifi信息获取（wifi列表、配置信息、热点信息）](https://blog.csdn.net/gao_chun/article/details/45891865)

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.widget.ImageView
import com.google.protobuf.Descriptors
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.LoggerInter
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.DEFAULT_TAG
import com.sysu.deepnavi.util.doNotNeedImpl

enum class Orientation {
    LEFT,
    FORWARD,
    RIGHT,
    TOP,
    BOTTOM,
    BACKWARD;

    override fun toString(): String = when (this) {
        LEFT -> "1"
        FORWARD -> "2"
        RIGHT -> "4"
        TOP -> "8"
        BOTTOM -> "16"
        BACKWARD -> "32"
    }

    fun toInt(): Int = when (this) {
        LEFT -> 1
        FORWARD -> 2
        RIGHT -> 4
        TOP -> 8
        BOTTOM -> 16
        BACKWARD -> 32
    }

    companion object {
        fun fromString(s: String): Orientation = when (s) {
            "1" -> LEFT
            "2" -> FORWARD
            "4" -> RIGHT
            "8" -> TOP
            "16" -> BOTTOM
            "32" -> BACKWARD
            else -> FORWARD
        }
    }
}

class DeepNaviManager private constructor() : SensorEventListener {
    private var descriptor: Descriptors.Descriptor = Basic.DeepNaviReq.getDescriptor()
    private val dataList: MutableMap<Descriptors.FieldDescriptor, DataCollectorInter<Any>> = mutableMapOf()

    private var running: Boolean = false
    private var thread: Thread? = null

    private var sensorManager: SensorManager? = null
    private var socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>? = null
    private var interval: Long = defaultInterval

    companion object {
        private const val defaultInterval: Long = 1000 / 3
        private var instance: DeepNaviManager? = null
            get() {
                if (field == null) field = DeepNaviManager()
                return field
            }

        @Synchronized
        fun get(): DeepNaviManager = instance!!

        var logger: LoggerInter? = null
    }

    private var hasInited = false
    fun init(context: Context, socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>, interval: Long = defaultInterval): DeepNaviManager {
        if (hasInited) {
            return this
        }
        return forceInit(context, socket, interval)
    }

    private var view: View? = null
    fun view(view: View): DeepNaviManager {
        this.view = view
        return this
    }

    fun forceInit(context: Context, socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>, interval: Long): DeepNaviManager {
        hasInited = true
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.socket = socket
        this.interval = interval
        logger?.d(DEFAULT_TAG, "DeepNaviManager.init(interval: %d)", interval)
        return this
    }

    fun getSensorManager() = sensorManager!!

    fun addDataCollector(dataCollector: DataCollectorInter<Any>) {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.addDataCollector(field: %s)", dataCollector.field)
        val key = descriptor.findFieldByName(dataCollector.field)
        dataList[key] = dataCollector
    }

    fun removeDataCollector(field: String): DataCollectorInter<Any>? {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.removeDataCollector(type: %s)", field)
        val key = descriptor.findFieldByName(field)
        return dataList.remove(key) ?: return null
    }

    fun registerListener(sensor: Sensor?, samplingPeriodUs: Int): Boolean? {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.registerListener(sensor: %d, samplingPeriodUs: %d)", sensor?.type ?: -1, samplingPeriodUs)
        return sensorManager?.registerListener(this, sensor, samplingPeriodUs)
    }

    fun unregisterListener(): Unit? {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.unregisterListener")
        return sensorManager?.unregisterListener(this)
    }

    private fun send() {
        val reqBuilder = Basic.DeepNaviReq.newBuilder()
        reqBuilder.time = System.currentTimeMillis()
        if (socket?.isConnected == false || dataList.isEmpty()) {
            return
        }
        dataList.forEach { entry ->
            val field = entry.key
            val dataCollector = entry.value
            if (field.isRepeated) {
                val dataArray = dataCollector.getDataArray()
                dataArray?.forEach { value -> reqBuilder.addRepeatedField(field, value) }
                logger?.d(DEFAULT_TAG, "DeepNaviManager.send{time: %d}, field: %s, size: %d", reqBuilder.time, field.name, dataArray?.size ?: 0)
            } else {
                reqBuilder.setField(field, dataCollector.getData() ?: return)
                logger?.d(DEFAULT_TAG, "DeepNaviManager.send{time: %d}, field: %s, size: 1", reqBuilder.time, field.name)
            }
        }
        if (running) {
            try {
                socket!!.send(reqBuilder.build())
                logger?.d(DEFAULT_TAG, "DeepNaviManager.send{time: %d}, successfully", reqBuilder.time)
            } catch (e: Exception) {
                logger?.d(DEFAULT_TAG, "DeepNaviManager.send{time: %d}, failed", reqBuilder.time)
            }
        }
    }

    fun loop() {
        thread = Thread {
            logger?.d(DEFAULT_TAG, "DeepNaviManager.loop begin at %d", System.currentTimeMillis())
            socket!!.connect()
            running = true
            while (running) {
                Thread.sleep(interval)
                send()
            }
            logger?.d(DEFAULT_TAG, "DeepNaviManager.loop end at %d", System.currentTimeMillis())
        }
        thread!!.start()
    }

    fun stop() {
        running = false
        socket!!.close()
        thread = null
        logger?.d(DEFAULT_TAG, "DeepNaviManager.stop at %d", System.currentTimeMillis())
    }

    fun onMessage(res: Basic.DeepNaviRes) {
        val v = view ?: return
        val resId = when (Orientation.fromString(res.result)) {
            Orientation.LEFT -> R.drawable.go_left
            Orientation.FORWARD -> R.drawable.go_forward
            Orientation.RIGHT -> R.drawable.go_right
            Orientation.TOP -> R.drawable.go_up
            Orientation.BOTTOM -> R.drawable.go_down
            Orientation.BACKWARD -> R.drawable.go_backward
        }
        if (v is ImageView) {
            v.setImageResource(resId)
        } else {
            v.setBackgroundResource(resId)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor != null) {
            dataList.forEach { entry ->
                val dataCollector = entry.value
                if (dataCollector is SensorListener && sensor.type == dataCollector.type) dataCollector.onAccuracyChanged(sensor, accuracy)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            dataList.forEach { entry ->
                val dataCollector = entry.value
                if (dataCollector is SensorListener && event.sensor.type == dataCollector.type) dataCollector.onSensorChanged(event)
            }
        }
    }

    interface SensorListener<Data> : DataCollectorInter<Data> {
        val type: Int
        fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = doNotNeedImpl()
        fun onSensorChanged(event: SensorEvent)
    }
}
