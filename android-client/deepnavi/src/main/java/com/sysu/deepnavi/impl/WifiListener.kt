package com.sysu.deepnavi.impl

import com.sysu.deepnavi.inter.DataCollectorInter

class WifiListener : DataCollectorInter<ByteArray> {
    override val field: String = "wifi"

    override fun getData(): ByteArray? {
        TODO()
    }

    override fun getDataArray(): Array<ByteArray>? {
        TODO()
    }
}
