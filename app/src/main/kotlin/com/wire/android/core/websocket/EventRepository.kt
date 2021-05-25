package com.wire.android.core.websocket

import com.wire.android.core.websocket.data.Message
import kotlinx.coroutines.channels.Channel

interface EventRepository {
    fun startSocket(): Channel<Message>
    fun closeSocket()
}