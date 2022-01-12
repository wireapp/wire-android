package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import kotlinx.coroutines.flow.SharedFlow

interface WebSocketService {

    @Receive
    fun observeWebSocketEvent(): SharedFlow<WebSocket.Event>

    @Receive
    fun receiveEvent(): SharedFlow<EventResponse>
}
