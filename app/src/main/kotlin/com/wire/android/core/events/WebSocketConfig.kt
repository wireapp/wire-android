package com.wire.android.core.events

import com.wire.android.core.config.GlobalConfig

data class WebSocketConfig(
    val clientId: String,
    val socketUrl: String = GlobalConfig.WEB_SOCKET_URL + clientId,
    val throttleTimeout: Long = DEFAULT_THROTTLE_TIMEOUT
) {
    companion object {
        const val DEFAULT_THROTTLE_TIMEOUT = 1000L
    }
}
