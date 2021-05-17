package com.wire.android.core.websocket.data

import kotlinx.coroutines.channels.Channel

class EventRepository constructor(private val webSocketProvider: WebSocketProvider) {

    fun startSocket(): Channel<Message> = webSocketProvider.startSocket()

    fun closeSocket() {
        webSocketProvider.stopSocket()
    }
}
