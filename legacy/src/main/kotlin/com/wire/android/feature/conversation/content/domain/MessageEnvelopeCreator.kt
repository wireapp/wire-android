package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.android.feature.messaging.ClientPayload
import com.wire.android.feature.messaging.RecipientEntry

class MessageEnvelopeCreator(
    private val messageRepository: MessageRepository
) {
    suspend fun createOutgoingEnvelope(
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
}
