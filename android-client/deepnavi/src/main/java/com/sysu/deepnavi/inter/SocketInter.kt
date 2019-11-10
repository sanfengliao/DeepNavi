package com.sysu.deepnavi.inter;

interface SocketInter<Req, Res> {
    fun connect()
    fun close()

    // fun send(msg: ByteArray)
    // fun send(msg: String) = send(msg.toByteArray())
    fun send(req: Req)

    // fun onMessage(msg: ByteArray) = onMessage(String(msg))
    // fun onMessage(msg: String)
    fun onMessage(res: Res)

    // fun onError(error: Error) = doNotNeedImpl()
    // fun onFatalError(error: Error) = doNotNeedImpl()
    // fun onOpen() = doNotNeedImpl()
    // fun onClose() = doNotNeedImpl()
}
