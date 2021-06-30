package com.wire.android.core.events.handler

import com.wire.android.core.date.DateStringMapper
import com.wire.android.core.events.Event
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.Text

class MessageEventsHandler(
    private val messageRepository: MessageRepository,
    private val dateStringMapper: DateStringMapper
) : EventsHandler<Event.Conversation.Message> {
    override suspend fun subscribe(event: Event.Conversation.Message) {
        val message = Message(event.id, event.conversationId, event.userId, event.sender, event.content, Text, Sent, dateStringMapper.fromStringToOffsetDateTime(event.time))
        messageRepository.decryptMessage(message)
    }
}
