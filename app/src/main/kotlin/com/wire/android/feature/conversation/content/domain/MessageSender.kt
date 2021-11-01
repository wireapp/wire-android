package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.Unauthorized
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.network.NetworkHandler
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
    private val networkHandler: NetworkHandler,
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
            if (!networkHandler.isConnected()) {
                //No connection!
                return@suspending Either.Left(NetworkConnection)
            }

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
                .map { detailedContacts ->
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
    ): ChatMessageEnvelope {
        val entries = detailedContacts.fold(mutableListOf<RecipientEntry>()) { acc, detailedContact ->
            val clientEntries = detailedContact.clients.mapNotNull { contactClient ->
                messageRepository.encryptMessageContent(
                    senderUserId, detailedContact.contact.id,
                    contactClient.id, message.id, message.content
                ).fold({
                    //This encryption failure handling can be improved
                    //It is not breaking, nor needs special attention, as it will fail later as a "missing client" anyway.
                    //TODO: Mark message as unable to send due to encryption issues
                    null
                }, { it })?.let { encryptedMessage ->
                    ClientPayload(contactClient.id, encryptedMessage.data)
                }
            }

            acc.also { it.add(RecipientEntry(detailedContact.contact.id, clientEntries)) }
        }

        return ChatMessageEnvelope(senderClientId, entries)
    }

    companion object {
        private const val WORK_POOL_NAME = "message-sending-worker"
    }

}
