package com.wire.wireone

internal interface KaliumProvider {
    fun statusLine(): String
}

internal expect fun createKaliumProvider(): KaliumProvider
