package com.wire.android.core.websocket.data

import com.wire.android.core.websocket.EventRepository
import kotlinx.coroutines.channels.Channel

class WebSocketDataSource(private val webSocketProvider: WebSocketProvider) : EventRepository {
    override fun startSocket(): Channel<Message> = webSocketProvider.startSocket()

    override fun closeSocket() {
        webSocketProvider.stopSocket()
    }
}
