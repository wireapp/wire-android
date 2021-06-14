package com.wire.android.core.websocket.data

import com.wire.android.core.websocket.EventRepository
import kotlinx.coroutines.flow.Flow

class EventDataSource(private val webSocketWorkConnection: WebSocketConnection) : EventRepository {
    override fun events(): Flow<Message> = webSocketWorkConnection.connect().let { webSocketWorkConnection.socketFlow }
}
