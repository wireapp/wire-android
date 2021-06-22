package com.wire.android.feature.conversation.list.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationRepository

class GetConversationMembersUseCase(
    private val conversationRepository: ConversationRepository,
    private val contactRepository: ContactRepository
) : UseCase<List<Contact>, GetConversationMembersParams> {

    override suspend fun run(params: GetConversationMembersParams): Either<Failure, List<Contact>> = suspending {
        conversationRepository.conversationMemberIds(params.conversation).flatMap { memberIds ->
            contactRepository.contactsById(memberIds.toSet())
        }
    }
}

data class GetConversationMembersParams(val conversation: Conversation)
