package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.UserPreKeyInfo

class OutgoingMessageRecipientsRetriever(
    private val preKeyRepository: PreKeyRepository,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository
) {

    /**
     * Gets detailed contacts for sending a new message in a conversation.
     * Will
     */
    suspend fun prepareRecipientsForNewOutgoingMessage(
        senderUserId: String,
        conversationId: String
    ): Either<Failure, List<DetailedContact>> = suspending {
        conversationRepository.detailedConversationMembers(conversationId).flatMap { detailedContacts ->
            createSessionForMissingClientsIfNeeded(senderUserId, detailedContacts).map {
                detailedContacts
            }
        }
    }

    private suspend fun createSessionForMissingClientsIfNeeded(
        senderUserId: String,
        detailedContacts: List<DetailedContact>
    ): Either<Failure, Unit> = suspending {
        missingContactClients(detailedContacts, senderUserId).flatMap { missingContactClients ->
            establishMissingSessions(senderUserId, missingContactClients)
        }
    }

    private suspend fun establishMissingSessions(
        senderUserId: String, missingContactClients: MutableMap<String, List<String>>
    ): Either<Failure, Unit> = suspending {
        if (missingContactClients.isEmpty()) {
            return@suspending Either.Right(Unit)
        }
        preKeyRepository.preKeysOfClientsByUsers(missingContactClients).map { preKeyInfoList: List<UserPreKeyInfo> ->
            preKeyInfoList.forEach { userPreKeyInfo ->
                userPreKeyInfo.clientsInfo.foldToEitherWhileRight(Unit) { clientPreKeyInfo, _ ->
                    messageRepository.establishCryptoSession(
                        senderUserId,
                        userPreKeyInfo.userId,
                        clientPreKeyInfo.clientId,
                        clientPreKeyInfo.preKey
                    )
                }
            }
        }
    }

    private suspend fun missingContactClients(detailedContacts: List<DetailedContact>, senderUserId: String) =
        suspending {
            detailedContacts.foldToEitherWhileRight(mutableMapOf<String, List<String>>()) { detailedContact, userAcc ->
                missingClientsForContact(detailedContact, senderUserId).map { missingClients ->
                    if (missingClients.isNotEmpty()) {
                        userAcc[detailedContact.contact.id] = missingClients
                    }
                    userAcc
                }
            }
        }

    private suspend fun missingClientsForContact(detailedContact: DetailedContact, senderUserId: String) =

        suspending {
            detailedContact.clients.foldToEitherWhileRight(mutableListOf<String>()) { contact, accumulated ->
                messageRepository.doesCryptoSessionExists(senderUserId, detailedContact.contact.id, contact.id)
                    .map { exists ->
                        if (!exists) {
                            accumulated += contact.id
                        }
                        accumulated
                    }
            }
        }
}
