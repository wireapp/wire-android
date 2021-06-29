package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.data.ConversationRepository

class SyncAllConversationMembersUseCase(
    private val conversationRepository: ConversationRepository,
    private val  contactRepository: ContactRepository
) : UseCase<Unit, Unit> {

    override suspend fun run(params: Unit): Either<Failure, Unit> = suspending {
        conversationRepository.allConversationMemberIds().flatMap {
            contactRepository.fetchContactsById(it.toSet())
        }
    }
}
