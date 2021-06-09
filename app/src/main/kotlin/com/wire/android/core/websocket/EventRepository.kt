package com.wire.android.core.websocket

import com.wire.android.core.websocket.data.Message
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun startSocket(): Flow<Message>
    fun closeSocket()
}
