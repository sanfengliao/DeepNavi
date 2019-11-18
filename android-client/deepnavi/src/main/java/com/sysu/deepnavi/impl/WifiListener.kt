package com.sysu.deepnavi.impl

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import com.sysu.deepnavi.inter.DataCollectorInter
import java.util.*

class WifiListener(context: Context) : DataCollectorInter<Int> {
    override val field: String = "wifiList"
    private var wifiManager: WifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
    @Volatile
    private var data: Int = 0
    private val dataList: LinkedList<Int> = LinkedList()

    override fun getData(): Int? {
        val wifiInfo = wifiManager.connectionInfo  // 当前wifi连接信息
        data = wifiInfo.rssi
        return data
    }

    private fun getAllData() {
        val wifiInfo = wifiManager.connectionInfo  // 当前wifi连接信息
        data = wifiInfo.rssi
        dataList.clear()
        dataList.addLast(data)
        val scanResults = wifiManager.scanResults  // 搜索到的设备列表
        scanResults.forEach {
            dataList.addLast(WifiManager.calculateSignalLevel(it.level, 4))
        }
    }

    override fun getDataArray(): Array<Int>? {
        getAllData()
        return dataList.toTypedArray()
    }
}
