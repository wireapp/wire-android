package com.wire.android.core.websocket

import com.tinder.scarlet.ws.Receive
import com.wire.android.core.events.datasource.remote.EventResponse
import kotlinx.coroutines.flow.Flow

interface WebSocketService {
    @Receive
    fun receiveEvent(): Flow<EventResponse>
}
