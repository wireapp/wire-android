package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.remote.WebSocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class EventDataSource(private val webSocketService: WebSocketService) : EventRepository {
    override fun events(): Flow<Event> = flow {
        webSocketService.receiveEvent().collect {
            it.payload?.let { payloads ->
                for (payload in payloads)
                    if (payload.type == NEW_MESSAGE_TYPE && payload.data != null)
                        emit(
                            Event.Conversation.MessageEvent(
                                it.id,
                                payload.conversation,
                                payload.data.sender,
                                payload.from,
                                payload.data.text,
                                payload.time
                            )
                        )
            }
        }
    }

    companion object {
        const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }
}
