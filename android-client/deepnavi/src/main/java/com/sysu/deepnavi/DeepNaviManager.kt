@file:Suppress("unused")

package com.sysu.deepnavi

// [几种常用的传感器（加速度传感器、重力传感器、方向传感器、陀螺仪）简介](https://blog.csdn.net/LEON1741/article/details/77200220) finished
// [Android 传感器开发详解](https://blog.csdn.net/Airsaid/article/details/52902299) finished
// [Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](https://blog.csdn.net/flowingflying/article/details/43233315)
// [地磁传感器 Pro Android学习笔记（一五五）：传感器（5）： 磁场传感器和方位（上）](http://www.manew.com/blog-166329-34441.html)
// [Pro Android学习笔记](https://blog.csdn.net/flowingflying/article/details/6212512)
// [Android wifi属性简介 及 wifi信息获取（wifi列表、配置信息、热点信息）](https://blog.csdn.net/gao_chun/article/details/45891865)

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.widget.ImageView
import com.google.protobuf.Descriptors
import com.liang.example.net.doPostAsync
import com.sysu.deepnavi.bean.Basic
import com.sysu.deepnavi.impl.ImageListener
import com.sysu.deepnavi.impl.ImageListener2
import com.sysu.deepnavi.impl.SensorListeners
import com.sysu.deepnavi.impl.WifiListener
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.inter.LoggerInter
import com.sysu.deepnavi.inter.SocketInter
import com.sysu.deepnavi.util.AndroidLogLogger
import com.sysu.deepnavi.util.DEFAULT_TAG
import com.sysu.deepnavi.util.MutableSize
import com.sysu.deepnavi.util.PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE
import com.sysu.deepnavi.util.camera2SupportInfo
import com.sysu.deepnavi.util.doNotNeedImpl

enum class Orientation {
    LEFT,
    FORWARD,
    RIGHT,
    UP,
    DOWN,
    BACKWARD;

    override fun toString(): String = when (this) {
        LEFT -> "1"
        FORWARD -> "2"
        RIGHT -> "4"
        UP -> "8"
        DOWN -> "16"
        BACKWARD -> "32"
    }

    fun toInt(): Int = when (this) {
        LEFT -> 1
        FORWARD -> 2
        RIGHT -> 4
        UP -> 8
        DOWN -> 16
        BACKWARD -> 32
    }

    companion object {
        fun fromString(s: String): Orientation = when (s) {
            "1" -> LEFT
            "2" -> FORWARD
            "4" -> RIGHT
            "8" -> UP
            "16" -> DOWN
            "32" -> BACKWARD
            else -> FORWARD
        }

        fun fromXRotation(r: Float) = when {
            r == 180f -> BACKWARD
            r == 0f -> FORWARD
            r > 0f -> RIGHT
            else -> LEFT
        }

        fun fromYRotation(r: Float) = when {
            r == 180f -> BACKWARD
            r == 0f -> FORWARD
            r > 0f -> UP
            else -> DOWN
        }
    }
}

class DeepNaviManager private constructor() : SensorEventListener {
    val reqDescriptor: Descriptors.Descriptor = Basic.DeepNaviReq.getDescriptor()
    private val dataList: MutableMap<Descriptors.FieldDescriptor, DataCollectorInter<Any>> = mutableMapOf()

    private var running: Boolean = false
    private var thread: Thread? = null

    private var sensorManager: SensorManager? = null
    private var socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>? = null
    private var interval: Long = defaultInterval

    var pathId: String? = null

    private var hasInited = false
    val isInited: Boolean
        get() = hasInited

    private var signalsListener: SignalsListener? = null
    fun setSignalsListener(signalsListener: SignalsListener?) {
        this.signalsListener = signalsListener
    }

    companion object {
        const val DEFAULT_VALUE_FREQUENCY = 3L
        const val DEFAULT_VALUE_SENSOR_RATE = 50  // 1s = 1000ms = 1000 * 1000us ，然后每秒50次
        val DEFAULT_PICTURE_SIZE: MutableSize = MutableSize(1080, 1920)

        private const val defaultInterval: Long = 1000 / DEFAULT_VALUE_FREQUENCY
        private var instance: DeepNaviManager? = null
            get() {
                if (field == null) field = DeepNaviManager()
                return field
            }

        @Synchronized
        fun get(): DeepNaviManager = instance!!

        var logger: LoggerInter? = null

        fun config(
            activity: Activity,
            configDataSet: Set<String> = setOf(),
            imageSize: MutableSize = DEFAULT_PICTURE_SIZE,
            previewView: View? = null,
            interval: Long = defaultInterval,
            socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> = object : SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes> {
                override val isConnected: Boolean
                    get() = true

                override fun connect() = Unit
                override fun close() = send(Basic.DeepNaviReq.newBuilder().setId(deepNaviId).build())  // 发送空的表示结束
                override fun onMessage(res: Basic.DeepNaviRes) = get().onMessage(res)
                override fun send(req: Basic.DeepNaviReq) = doPostAsync(url, null, req.toByteArray()) {
                    onMessage(Basic.DeepNaviRes.parseFrom(it?.content ?: return@doPostAsync))
                }
            },
            deepNaviId: String = "default",
            url: String = ""
        ): DeepNaviManager {
            if (logger == null) {
                logger = AndroidLogLogger()
            }
            val result = get()
            result.init(activity, socket, interval)
            SensorListeners.initAll(configDataSet)
            if ("image" in configDataSet) {
                val imageListener = when {
                    camera2SupportInfo(activity) == 2 -> ImageListener2(activity, previewView, pictureSize = imageSize)
                    else -> ImageListener(activity, previewView, pictureSize = imageSize)
                }
                result.addDataCollector(imageListener as DataCollectorInter<Any>)
            }
            if ("wifiList" in configDataSet) {
                result.addDataCollector(WifiListener(activity) as DataCollectorInter<Any>)
            }
            return result
        }

        fun start(
            rates: Map<String, Int> = SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG.map { it to 1000000 / 50 }.toMap(),
            registerList: Set<String> = SensorListeners.DEFAULT_VALUE_SENSOR_CONFIG
        ) {
            val deepNaviManager = get()
            val imageListener = deepNaviManager.imageListener
            val imageListener2 = deepNaviManager.imageListener2
            if (imageListener != null && !imageListener.haveOpenCamera) {
                imageListener.cameraUtil.openCamera()
                imageListener.cameraUtil.startPreview()
            } else if (imageListener2 != null) {
                if (!imageListener2.haveOpenCamera) {
                    imageListener2.cameraUtil.openCamera()
                }
                imageListener2.cameraUtil.startPreview()
            }
            SensorListeners.registerAll(rates, registerList)
            deepNaviManager.loop()
        }

        fun stop() {
            val deepNaviManager = get()
            deepNaviManager.imageListener2?.cameraUtil?.stopPreview()
            deepNaviManager.stop()
            SensorListeners.unregisterAll()
        }

        fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
            if (requestCode == PERMISSION_CAMERA_AND_STORAGE_REQUEST_CODE
                && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
            ) {
                val deepNaviManager = get()
                val imageListener = deepNaviManager.imageListener
                val imageListener2 = deepNaviManager.imageListener2
                if (imageListener != null && !imageListener.haveOpenCamera) {
                    imageListener.cameraUtil.openCamera()
                    imageListener.cameraUtil.startPreview()
                }
                if (imageListener2 != null && !imageListener2.haveOpenCamera) {
                    imageListener2.cameraUtil.openCamera()
                    imageListener2.cameraUtil.startPreview()
                }
            }
        }
    }

    fun init(context: Context, socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>, interval: Long = defaultInterval): DeepNaviManager {
        if (hasInited) {
            return this
        }
        return reInit(context, socket, interval)
    }

    private var directionPanel: View? = null
    fun directionPanel(view: View): DeepNaviManager {
        this.directionPanel = view
        return this
    }

    fun reInit(context: Context, socket: SocketInter<Basic.DeepNaviReq, Basic.DeepNaviRes>, interval: Long): DeepNaviManager {
        hasInited = true
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.socket = socket
        this.interval = interval
        logger?.d(DEFAULT_TAG, "DeepNaviManager.init(interval: %d)", interval)
        return this
    }

    fun getSensorManager() = sensorManager!!
    val useCamera2: Boolean
        get() = dataList[reqDescriptor.findFieldByName("image")] is ImageListener2
    val imageListener: ImageListener?
        get() = dataList[reqDescriptor.findFieldByName("image")] as? ImageListener
    val imageListener2: ImageListener2?
        get() = dataList[reqDescriptor.findFieldByName("image")] as? ImageListener2

    fun addDataCollector(dataCollector: DataCollectorInter<Any>) {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.addDataCollector(field: %s)", dataCollector.field)
        val key = reqDescriptor.findFieldByName(dataCollector.field)
        dataList[key] = dataCollector
    }

    fun removeDataCollector(field: String): DataCollectorInter<Any>? {
        logger?.d(DEFAULT_TAG, "DeepNaviManager.removeDataCollector(type: %s)", field)
        val key = reqDescriptor.findFieldByName(field)
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
        val flag = socket?.isConnected == false
        if (flag && signalsListener == null || dataList.isEmpty()) {
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
        if (pathId != null) {
            reqBuilder.id = pathId
        }
        val req = reqBuilder.build()
        signalsListener?.onSignals(req)
        if (flag) {
            return
        }
        if (running) {
            try {
                socket!!.send(req)
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
        val v = directionPanel ?: return
        val resId = when (Orientation.fromXRotation(res.rotation)) {
            Orientation.LEFT -> R.drawable.go_left
            Orientation.FORWARD -> R.drawable.go_forward
            Orientation.RIGHT -> R.drawable.go_right
            Orientation.UP -> R.drawable.go_up
            Orientation.DOWN -> R.drawable.go_down
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

    interface SignalsListener {
        fun onSignals(req: Basic.DeepNaviReq)
    }
}
