package com.wire.android.feature.conversation.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationResponse
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsResponse

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationRemoteDataSource: ConversationsRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationsRepository {

    override suspend fun fetchConversations(): Either<Failure, Unit> = fetchRemoteConversations()

    private suspend fun fetchRemoteConversations(start: String? = null): Either<Failure, Unit> = suspending {
        conversationRemoteDataSource.conversationsByBatch(start, CONVERSATION_REQUEST_PAGE_SIZE).flatMap { response ->
            saveRemoteConversations(response.conversations).flatMap {
                fetchConversationsNextPageIfExists(response)
            }
        }
    }

    private suspend fun saveRemoteConversations(conversations: List<ConversationResponse>) = suspending {
        val entities = conversationMapper.fromConversationResponseListToEntityList(conversations)
        conversationLocalDataSource.saveConversations(entities).flatMap {
            saveConversationMemberIds(conversations)
        }
    }

    private suspend fun saveConversationMemberIds(conversations: List<ConversationResponse>): Either<Failure, Unit> {
        val conversationMembers =
            conversationMapper.fromConversationResponseListToConversationMembers(conversations)
        return conversationLocalDataSource.saveMemberIdsForConversations(conversationMembers)
    }

    private suspend fun fetchConversationsNextPageIfExists(response: ConversationsResponse) =
        if (response.hasMore) {
            val nextPageStartId = response.conversations.last().id
            fetchRemoteConversations(nextPageStartId)
        } else {
            Either.Right(Unit)
        }

    override suspend fun conversationMemberIds(conversation: Conversation): Either<Failure, List<String>> =
        conversationLocalDataSource.conversationMemberIds(conversation.id)

    override suspend fun allConversationMemberIds(): Either<Failure, List<String>> =
        conversationLocalDataSource.allConversationMemberIds()

    companion object {
        private const val CONVERSATION_REQUEST_PAGE_SIZE = 100
    }
}
