package com.sysu.deepnavi.impl

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.DEFAULT_TAG
import java.util.*

@Suppress("UNCHECKED_CAST", "unused")
class SensorListener2<Data>(override val type: Int, override val field: String, val action: (event: SensorEvent) -> Data) :
    DeepNaviManager.SensorListener<Data> {
    init {
        DeepNaviManager.logger?.d(DEFAULT_TAG, "SensorListener2.constructor(type: %d, field: %s)", type, field)
    }

    @Volatile
    private var data: Data? = null
    private val dataList: LinkedList<Data> = LinkedList()
    private var maxSize: Int = 0

    fun init(rate: Int, add: Boolean = false): SensorListener2<Data> {
        val result: Boolean = DeepNaviManager.get().registerListener(DeepNaviManager.get().getSensorManager().getDefaultSensor(type), rate) ?: false
        if (add) DeepNaviManager.get().addDataCollector(this as DataCollectorInter<Any>)
        DeepNaviManager.logger?.d(DEFAULT_TAG, "SensorListener2.init(rate: %d) -- result: %s", rate, result.toString())
        return this
    }

    fun init(sensor: Sensor, rate: Int, add: Boolean = false): SensorListener2<Data> {
        val result: Boolean = DeepNaviManager.get().registerListener(sensor, rate) ?: false
        if (add) DeepNaviManager.get().addDataCollector(this as DataCollectorInter<Any>)
        DeepNaviManager.logger?.d(DEFAULT_TAG, "SensorListener2.init(rate: %d) -- result: %s", rate, result.toString())
        return this
    }

    override fun onSensorChanged(event: SensorEvent) {
        data = action(event)
        synchronized(dataList) {
            dataList.addFirst(data)
            if (maxSize != 0 && dataList.size > maxSize) dataList.removeLast()
        }
    }

    override fun getDataArray(): Array<Data> {
        synchronized(dataList) {
            return dataList.toArray() as Array<Data>
        }
    }

    override fun getData(): Data? {
        return data
    }
}

// class MagneticListener(rate: Int = 1000000 / 50) : DeepNaviManager.SensorListener<Basic.MagneticReq> {
//     override val type: Int = Sensor.TYPE_MAGNETIC_FIELD  // TYPE_MAGNETIC_FIELD_UNCALIBRATED
//     override val field: String = "magnetic"
//     val dataList: MutableList<Basic.MagneticReq> = mutableListOf()
//
//     init {
//         DeepNaviManager.get()
//             .registerListener(
//                 DeepNaviManager.get()
//                     .getSensorManager()
//                     .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
//                 rate
//             )
//     }
//
//     override fun onSensorChanged(event: SensorEvent) {
//         val values = event.values
//         dataList.add(
//             Basic.MagneticReq.newBuilder()
//                 .setMagneticX(values[0])
//                 .setMagneticY(values[1])
//                 .setMagneticZ(values[2])
//                 .build()
//         )
//     }
//
//     override fun getDataArray(): Array<Basic.MagneticReq>? {
//         val result: Array<Basic.MagneticReq> = dataList.toTypedArray()
//         dataList.clear()
//         return result
//     }
// }
