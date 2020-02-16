package com.sysu.deepnavi.impl

import android.hardware.Sensor
import android.hardware.SensorEvent
import com.sysu.deepnavi.DeepNaviManager
import com.sysu.deepnavi.inter.DataCollectorInter
import com.sysu.deepnavi.util.DEFAULT_TAG
import java.util.*

@Suppress("UNCHECKED_CAST", "unused")
class SensorListener2<Data>(
    override val type: Int,
    override val field: String,
    val action: (event: SensorEvent) -> Data
) : DeepNaviManager.SensorListener<Data> {
    init {
        DeepNaviManager.logger?.d(DEFAULT_TAG, "SensorListener2.constructor(type: %d, field: %s)", type, field)
    }

    @Volatile
    private var data: Data? = null
    private val dataList: LinkedList<Data> = LinkedList()
    var maxSize: Int = 0

    fun init(): SensorListener2<Data> {
        DeepNaviManager.get().addDataCollector(this as DataCollectorInter<Any>)
        DeepNaviManager.logger?.d(DEFAULT_TAG, "SensorListener2.init")
        return this
    }

    fun register(rate: Int) = DeepNaviManager.get().registerListener(DeepNaviManager.get().getSensorManager().getDefaultSensor(type), rate) ?: false
    fun register(sensor: Sensor, rate: Int) = DeepNaviManager.get().registerListener(sensor, rate) ?: false

    override fun onSensorChanged(event: SensorEvent) {
        data = action(event)
        synchronized(dataList) {
            dataList.addFirst(data)
            if (maxSize != 0 && dataList.size > maxSize) {
                dataList.removeLast()
            }
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
