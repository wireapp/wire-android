package com.wire.wireone

internal actual fun createKaliumProvider(): KaliumProvider = object : KaliumProvider {
    override fun statusLine(): String = "Kalium: not available on Web (kalium/logic has JS disabled)"
}
