package com.wire.android.core.events.handler

import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.events.Event
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.DefaultUseCaseExecutor
import com.wire.android.core.usecase.UseCaseExecutor
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationSummaryBuilder
import com.wire.android.shared.notification.usecase.ShouldDisplayNotificationUseCase
import com.wire.android.shared.notification.usecase.ShouldDisplayNotificationUseCaseParams

class MessageEventsHandler(
    override val dispatcherProvider: DispatcherProvider,
    private val shouldDisplayNotificationUseCase: ShouldDisplayNotificationUseCase,
    private val messageRepository: MessageRepository,
    private val messageMapper: MessageMapper,
    private val notificationSummaryBuilder: NotificationSummaryBuilder,
    private val notificationBuilder: NotificationBuilder
) : EventsHandler<Event.Conversation.MessageEvent>, UseCaseExecutor by DefaultUseCaseExecutor(dispatcherProvider)  {
    override suspend fun subscribe(event: Event.Conversation.MessageEvent) {
        val message = messageMapper.fromMessageEventToEncryptedMessageEnvelope(event)
        messageRepository.receiveEncryptedMessage(message)
        displayNotification(message.conversationId)
    }

    private suspend fun displayNotification(conversationId: String) = suspending {
        val shouldDisplayNotificationUseCaseParams = ShouldDisplayNotificationUseCaseParams(conversationId)
        shouldDisplayNotificationUseCase.run(shouldDisplayNotificationUseCaseParams).map { shouldDisplayNotification ->
            if(shouldDisplayNotification) {
                val messages = messageRepository.latestUnreadMessages(conversationId)
                messages.map {
                    notificationSummaryBuilder.createSummaryNotification()
                    //TODO pass conversation name in next PR
                    notificationBuilder.displayNotification(conversationId, "Conversation Name", it)
                }
            }
        }
    }
}
