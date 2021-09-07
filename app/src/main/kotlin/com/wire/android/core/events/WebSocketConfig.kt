package com.wire.android.core.events

data class WebSocketConfig(
    private val baseUrl: String,
    val throttleTimeout: Long = DEFAULT_THROTTLE_TIMEOUT
) {

    fun urlForClient(clientId: String) = baseUrl + clientId

    companion object {
        const val DEFAULT_THROTTLE_TIMEOUT = 1000L
    }
}
