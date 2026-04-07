package com.wire.wireone

internal actual fun createKaliumProvider(): KaliumProvider = object : KaliumProvider {
    override fun statusLine(): String = "Kalium: status not wired for Android sample"
}
