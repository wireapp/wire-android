package com.wire.android.core.events.handler

import com.wire.android.core.events.Event
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.mapper.MessageMapper

class MessageEventsHandler(
    private val messageRepository: MessageRepository,
    private val messageMapper: MessageMapper
) : EventsHandler<Event.Conversation.MessageEvent> {
    override suspend fun subscribe(event: Event.Conversation.MessageEvent) {
        val message = messageMapper.fromMessageEventToEncryptedMessageEnvelope(event)
        messageRepository.receiveEncryptedMessage(message)
    }
}
