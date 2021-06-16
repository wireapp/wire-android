package com.wire.android.core.websocket

import com.tinder.scarlet.Event
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow

interface WebSocketService {
    @Receive
    fun observeEvent(): Flow<Event>
}
