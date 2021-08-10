package com.wire.android.core.events.mapper

import com.wire.android.core.events.Event
import com.wire.android.core.events.datasource.remote.Payload

class EventMapper {
    fun eventFromPayload(payload: Payload, eventId: String): Event =
        when (payload.type) {
            NEW_MESSAGE_TYPE -> {
                with(payload) {
                    data?.let {
                        Event.Conversation.MessageEvent(eventId, conversation, it.sender, from, it.text, time)
                    } ?: Event.Unknown
                }
            }
            else -> Event.Unknown
        }

    companion object {
        const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }
}
