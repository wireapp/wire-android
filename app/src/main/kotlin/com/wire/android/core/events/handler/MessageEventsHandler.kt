package com.wire.android.core.events.handler

import android.util.Log
import com.wire.android.core.events.Event
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.Text

class MessageEventsHandler(private val messageRepository: MessageRepository) :
    EventsHandler<Event.Conversation.Message> {
    override suspend fun subscribe(event: Event.Conversation.Message) {
        Log.d("TAG", "handle: $event")
        val message = Message(
            event.conversationId,
            event.conversationId,
            event.content,
            Text,
            Sent,
            event.time,
            event.userId,
            event.sender
        )
        messageRepository.decryptMessage(message)
    }
}
