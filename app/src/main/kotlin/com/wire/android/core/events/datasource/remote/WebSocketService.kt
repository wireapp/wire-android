package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.ws.Receive
import kotlinx.coroutines.flow.Flow

interface WebSocketService {
    @Receive
    fun receiveEvent(): Flow<EventResponse>
}
