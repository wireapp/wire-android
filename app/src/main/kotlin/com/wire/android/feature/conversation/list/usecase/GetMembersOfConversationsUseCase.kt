package com.wire.android.feature.conversation.list.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactRepository
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository

//TODO: test
class GetMembersOfConversationsUseCase(
    private val conversationsRepository: ConversationsRepository,
    private val contactRepository: ContactRepository
) : UseCase<Map<Conversation, List<Contact>>, GetMembersOfConversationsParams> {

    override suspend fun run(params: GetMembersOfConversationsParams): Either<Failure, Map<Conversation, List<Contact>>> {
        val conversationAndMemberIds: Map<Conversation, List<String>> = params.conversations.map { conversation ->

            val memberIdsToRequest = getConversationMemberIds(conversation).sorted().take(params.maxMemberCountPerConversation)
            conversation to memberIdsToRequest

        }.toMap()

        val memberIdsForAllConversations = conversationAndMemberIds.values.flatten().toSet()

        return contactRepository.contactsById(memberIdsForAllConversations).map { contacts ->
            conversationAndMemberIds.mapValues { (_, memberIds) ->
                contacts.filter { memberIds.contains(it.id) }
            }
        }
    }

    private suspend fun getConversationMemberIds(conversation: Conversation): List<String> =
        conversationsRepository.conversationMemberIds(conversation).fold({ emptyList() }) { it }!!
}

data class GetMembersOfConversationsParams(
    val conversations: List<Conversation>,
    val maxMemberCountPerConversation: Int = Integer.MAX_VALUE
)
