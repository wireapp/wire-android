package com.wire.android.core.websocket.data

import com.tinder.scarlet.Message
import com.tinder.scarlet.WebSocket
import com.wire.android.core.events.Event
import com.wire.android.core.websocket.EventRepository
import com.wire.android.core.websocket.WebSocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventDataSource(private val webSocketService: WebSocketService) : EventRepository {
    override fun events(): Flow<Event> =
        webSocketService.observeEvent().map { event ->
            if (event is com.tinder.scarlet.Event.OnWebSocket.Event<*> && event.event is WebSocket.Event.OnMessageReceived) {
                (event.event as WebSocket.Event.OnMessageReceived).message.let {
                    if (it is Message.Bytes ) {
                        //TODO handle events based on event type
                        return@map Event.Message(0, it.value.toString(charset))
                    }
                }
            }
            return@map Event.Unknown
        }

    companion object {
        private val charset = Charsets.UTF_8
    }
}
