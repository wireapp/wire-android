package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.OneToOne
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class RefineConversationNamesUseCase(
    private val conversationListRepository: ConversationListRepository,
    private val conversationRepository: ConversationRepository
) : UseCase<Unit, Unit> {

    override suspend fun run(params: Unit): Either<Failure, Unit> = suspending {
        conversationRepository.numberOfConversations().flatMap {
            updateConversationNamesByBatch(0, CONVERSATION_LIST_QUERY_SIZE, it)
        }
    }

    private suspend fun updateConversationNamesByBatch(
        start: Int,
        count: Int,
        totalCount: Int
    ): Either<Failure, Unit> = suspending {
        if (start >= totalCount) Either.Right(Unit)
        else conversationListRepository.conversationListInBatch(start, count)
            .flatMap { updateConversationNamesIfNecessary(it) }
            .flatMap { updateConversationNamesByBatch(start + count, count, totalCount) }
    }

    private suspend fun updateConversationNamesIfNecessary(items: List<ConversationListItem>): Either<Failure, Unit> {
        val conversationsToUpdate = items.mapNotNull { item ->
            newConversationName(item.conversation, item.members)?.let {
                item.conversation.copy(name = it)
            }
        }

        return if (conversationsToUpdate.isEmpty()) Either.Right(Unit)
        else conversationRepository.updateConversations(conversationsToUpdate)
    }

    private fun newConversationName(conversation: Conversation, members: List<Contact>): String? =
        if (!conversation.name.isNullOrBlank() && conversation.type != OneToOne) {
            null
        } else if (members.isNotEmpty()) {
            concatMemberNames(members)
        } else null

    private fun concatMemberNames(members: List<Contact>): String = members.joinToString(separator = ", ") { it.name }

    companion object {
        private const val CONVERSATION_LIST_QUERY_SIZE = 20
    }
}
