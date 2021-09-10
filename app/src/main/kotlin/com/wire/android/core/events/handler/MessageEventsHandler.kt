package com.wire.android.core.events.handler

import com.wire.android.core.events.Event
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationSummaryBuilder

class MessageEventsHandler(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val messageMapper: MessageMapper,
    private val notificationSummaryBuilder: NotificationSummaryBuilder,
    private val notificationBuilder: NotificationBuilder
) : EventsHandler<Event.Conversation.MessageEvent> {

    override suspend fun subscribe(event: Event.Conversation.MessageEvent) {
        val message = messageMapper.fromMessageEventToEncryptedMessageEnvelope(event)
        messageRepository.receiveEncryptedMessage(message)
        displayNotification(message.conversationId)
    }

    private suspend fun displayNotification(conversationId: String) = suspending {
        conversationRepository.currentOpenedConversationId().map { currentOpenedConversationId ->
            if(currentOpenedConversationId != conversationId) {
                notificationSummaryBuilder.createSummaryNotification()
                val messages = messageRepository.latestUnreadMessages(conversationId)
                messages.map {
                    val conversationName = conversationRepository.conversationName(conversationId)
                    conversationName.map { name ->
                        notificationBuilder.displayNotification(conversationId, name, it)
                    }
                }
            }
        }
    }
}
