package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.SendMessageFailure
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.android.feature.messaging.ClientPayload
import com.wire.android.feature.messaging.RecipientEntry
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

class MessageSender(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val messageSendFailureHandler: MessageSendFailureHandler,
    private val outgoingMessageRecipientsRetriever: OutgoingMessageRecipientsRetriever
) {

    @ObsoleteCoroutinesApi
    //TODO: Replace with dispatcher.Default.limitedParallelism(1, WORK_POOL_NAME) when migrating to Coroutines 1.6
    private val context = newSingleThreadContext(WORK_POOL_NAME)

    suspend fun trySendingOutgoingMessage(senderUserId: String, messageId: String): Either<Failure, Unit> =
        suspending {
            val clientId = sessionRepository.userSession(senderUserId).coFold({ null }, { it.clientId })
                ?: return@suspending Either.Left(Unauthorized)

            //TODO Wait for sync to be done before sending things!
            withContext(context) {
                suspending {
                    messageRepository.messageById(messageId)
                        .flatMap { message -> getRecipientsAndAttemptSend(senderUserId, clientId, message) }
                }
            }
        }

    private suspend fun getRecipientsAndAttemptSend(senderUserId: String, clientId: String, message: Message): Either<Failure, Unit> =
        suspending {
            outgoingMessageRecipientsRetriever.prepareRecipientsForNewOutgoingMessage(senderUserId, message.conversationId)
                .flatMap { detailedContacts ->
                    createOutgoingEnvelope(detailedContacts, clientId, senderUserId, message)
                }.flatMap { envelope ->
                    sendEnvelopeRetryingIfPossible(message, envelope, senderUserId, clientId)
                }.flatMap {
                    messageRepository.markMessageAsSent(message.id)
                }
        }

    private suspend fun sendEnvelopeRetryingIfPossible(
        message: Message,
        envelope: ChatMessageEnvelope,
        senderUserId: String,
        clientId: String
    ) = suspending {
        when (val sendFailure = messageRepository.sendMessageEnvelope(message.conversationId, envelope).coFold({ it }, { null })) {
            null -> Either.Right(Unit)
            is SendMessageFailure.NetworkFailure -> Either.Left(NetworkConnection)
            is SendMessageFailure.ClientsHaveChanged -> {
                messageSendFailureHandler.handleClientsHaveChangedFailure(sendFailure).flatMap {
                    //TODO Optimize when trying again
                    //conserve partial encrypted envelope and reuse instead of re-encrypting everything
                    getRecipientsAndAttemptSend(senderUserId, clientId, message)
                }
            }
        }
    }

    private suspend fun createOutgoingEnvelope(
        detailedContacts: List<DetailedContact>,
        senderClientId: String,
        senderUserId: String,
        message: Message
    ): Either<Failure, ChatMessageEnvelope> = suspending {
        detailedContacts.foldToEitherWhileRight(mutableListOf<RecipientEntry>()) { detailedContact, recipientAccumulator ->
            detailedContact.clients.foldToEitherWhileRight(mutableListOf<ClientPayload>()) { client, clientAccumulator ->
                messageRepository.encryptMessageContent(
                    senderUserId, detailedContact.contact.id, client.id, message.id, message.content
                ).map { encryptedMessage ->
                    clientAccumulator.also {
                        it.add(ClientPayload(client.id, encryptedMessage.data))
                    }
                }
            }.map { clientEntries ->
                recipientAccumulator.also {
                    it.add(RecipientEntry(detailedContact.contact.id, clientEntries))
                }
            }
        }
    }.map { recipientEntries ->
        ChatMessageEnvelope(senderClientId, recipientEntries)
    }


    companion object {
        private const val WORK_POOL_NAME = "message-sending-worker"
    }

}
