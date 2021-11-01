package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.usecase.SendMessageWorkerScheduler

class SendMessageService(
    private val messageRepository: MessageRepository,
    private val sendMessageWorkerScheduler: SendMessageWorkerScheduler,
    private val messageSender: MessageSender
) {

    suspend fun sendOrScheduleNewMessage(message: Message): Either<Failure, Unit> = suspending {
        val senderUserId = message.senderUserId
        val messageId = message.id
        messageRepository.storeOutgoingMessage(message).flatMap {
            messageSender.trySendingOutgoingMessage(senderUserId, messageId)
        }.coFold({
            if (it is NetworkConnection) {
                sendMessageWorkerScheduler.scheduleMessageSendingWorker(senderUserId, messageId)
                return@coFold Either.Right(Unit)
            }
            return@coFold Either.Left(it)
        }, { Either.Right(Unit) }
        )!!
    }
}
