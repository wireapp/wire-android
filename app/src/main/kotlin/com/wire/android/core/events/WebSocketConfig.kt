package com.wire.android.core.events

import com.wire.android.core.config.GlobalConfig

data class WebSocketConfig(
    private val baseUrl: String = GlobalConfig.WEB_SOCKET_URL,
    val throttleTimeout: Long = DEFAULT_THROTTLE_TIMEOUT
) {

    fun urlForClient(clientId: String) = baseUrl + clientId

    companion object {
        const val DEFAULT_THROTTLE_TIMEOUT = 1000L
    }
}
