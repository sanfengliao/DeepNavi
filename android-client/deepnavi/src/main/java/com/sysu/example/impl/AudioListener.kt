package com.sysu.deepnavi.impl

import com.sysu.deepnavi.inter.DataCollectorInter

class AudioListener() : DataCollectorInter<ByteArray> {
    override val field: String = "image"

    override fun getData(): ByteArray? {
        TODO()
    }

    override fun getDataArray(): Array<ByteArray>? {
        TODO()
    }
}
