package com.wire.android.core.events.datasource

import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.remote.EventResponse
import com.wire.android.core.websocket.WebSocketService
import kotlinx.coroutines.flow.Flow

class EventDataSource(private val webSocketService: WebSocketService) : EventRepository {
    override fun events(): Flow<EventResponse> = webSocketService.receiveEvent()
}
