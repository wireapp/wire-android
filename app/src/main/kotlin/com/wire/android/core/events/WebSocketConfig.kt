package com.wire.android.core.events

import com.wire.android.core.config.GlobalConfig

data class WebSocketConfig(
    val clientId: String,
    val socketUrl: String = GlobalConfig.WEB_SOCKET_URL + clientId
) {
    companion object {
        const val THROTTLE_TIMEOUT = 1000L
    }
}
