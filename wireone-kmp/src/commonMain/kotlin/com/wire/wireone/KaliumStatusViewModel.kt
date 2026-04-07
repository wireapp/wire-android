package com.wire.wireone

internal class KaliumStatusViewModel(
    private val kaliumProvider: KaliumProvider = createKaliumProvider()
) {
    fun statusLine(): String = kaliumProvider.statusLine()
}
