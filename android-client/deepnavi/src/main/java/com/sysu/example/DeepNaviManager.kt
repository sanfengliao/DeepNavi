package com.sysu.deepnavi

// [几种常用的传感器（加速度传感器、重力传感器、方向传感器、陀螺仪）简介](https://blog.csdn.net/LEON1741/article/details/77200220) finished
// [Android 传感器开发详解](https://blog.csdn.net/Airsaid/article/details/52902299) finished
// [Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](https://blog.csdn.net/flowingflying/article/details/43233315)
// [地磁传感器 Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](http://www.manew.com/blog-166329-34441.html)
// [Pro Android学习笔记](https://blog.csdn.net/flowingflying/article/details/6212512)

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.protobuf.Descriptors
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.LoggerInter
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.DEFAULT_TAG
import com.sysu.deepnavi.util.doNotNeedImpl

class DeepNaviManager private constructor() : SensorEventListener {
    private var descriptor: Descriptors.Descriptor = Basic.DeepNaviReq.getDescriptor()
    private val dataList: MutableMap<Descriptors.FieldDescriptor, DataCollectorInter<Any>> = mutableMapOf()
    // TODO: sensorDataList
    private var running: Boolean = false
    var thread: Thread? = null

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

    fun init(context: Context, socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>, interval: Long = defaultInterval) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.socket = socket
        this.interval = interval
        logger?.d(DEFAULT_TAG, "DeepNaviManager.init(interval: %d)", interval)
    }

    fun getSensorManager() = sensorManager!!

    fun addDataCollector(dataCollector: DataCollectorInter<Any>) {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.addDataCollector(field: %s)", dataCollector.field)
        dataList.put(descriptor.findFieldByName(dataCollector.field), dataCollector)
    }

    fun removeDataCollector(field: String): DataCollectorInter<Any>? {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.removeDataCollector(type: %s)", field)
        return dataList.remove(descriptor.findFieldByName(field))
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
        dataList.forEach { entry ->
            val field = entry.key
            val dataCollector = entry.value
            if (field.isRequired) {
                dataCollector.getDataArray()?.forEach { value -> reqBuilder.addRepeatedField(field, value) }
            } else reqBuilder.setField(field, dataCollector.getData())
        }
        logger?.d(DEFAULT_TAG, "DeepNaviManager.send{time: %d}", reqBuilder.time)
        if (running) socket!!.send(reqBuilder.build())
    }

    fun loop() {
        thread = Thread {
            logger?.d(DEFAULT_TAG, "DeepNaviManager.loop begin at %d", System.currentTimeMillis())
            socket!!.connect()
            running = true
            while (running) {
                send()
                Thread.sleep(interval)
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int): Unit {
        if (sensor != null) {
            dataList.forEach { entry ->
                val dataCollector = entry.value
                if (dataCollector is SensorListener && sensor.type == dataCollector.type) dataCollector.onAccuracyChanged(sensor, accuracy)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?): Unit {
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
