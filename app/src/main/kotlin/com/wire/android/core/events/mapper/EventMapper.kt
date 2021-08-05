package com.wire.android.core.events.mapper

import com.wire.android.core.events.Event
import com.wire.android.core.events.datasource.remote.Payload

class EventMapper {
    fun eventFromPayload(payload: Payload, eventId: String) : Event {
        when (payload.type) {
            NEW_MESSAGE_TYPE -> {
                if(payload.data != null) {
                    return Event.Conversation.MessageEvent(
                        eventId,
                        payload.conversation,
                        payload.data.sender,
                        payload.from,
                        payload.data.text,
                        payload.time
                    )
                }
            }
        }
        return Event.Unknown
    }
    companion object {
        const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }
}
