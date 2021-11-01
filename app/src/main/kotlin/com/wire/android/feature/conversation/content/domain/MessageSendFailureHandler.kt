package com.wire.android.feature.conversation.content.domain

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.content.SendMessageFailure

class MessageSendFailureHandler(private val contactRepository: ContactRepository) {
    /**
     * Handle a failure when attempting to send a message
     * due to contacts and/or clients being removed from conversation and/or added to them.
     * @return Either.Left if can't recover from error
     * @return Either.Right if the error was properly handled and a new attempt at sending message can be made
     */
    suspend fun handleClientsHaveChangedFailure(sendFailure: SendMessageFailure.ClientsHaveChanged): Either<Failure, Unit> = suspending {
        //TODO Add/remove members to/from conversation
        //TODO remove clients from conversation
        contactRepository.fetchContactsById(sendFailure.missingClientsOfUsers.keys).flatMap {
            sendFailure.missingClientsOfUsers.entries.foldToEitherWhileRight(Unit) { entry, _ ->
                contactRepository.addNewClientsToContact(entry.key, entry.value.map(::ContactClient))
            }
        }
    }
}
