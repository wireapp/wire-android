package com.wire.android.feature.conversation.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationsRemoteDataSource

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationRemoteDataSource: ConversationsRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationsRepository {

    override suspend fun fetchConversations(start: String?, size: Int): Either<Failure, List<Conversation>> = suspending {
        conversationRemoteDataSource.conversationsByBatch(start, size).flatMap { response ->
            val conversationEntities = conversationMapper.fromConversationResponseToEntityList(response)
            conversationLocalDataSource.saveConversations(conversationEntities).flatMap {
                val conversationMemberEntities = conversationMapper.fromConversationResponseToConversationMembers(response)
                conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities).map {
                    conversationEntities.map { conversationMapper.fromEntity(it) }
                }
            }
        }
    }

    override suspend fun conversationMemberIds(conversation: Conversation): Either<Failure, List<String>> =
        conversationLocalDataSource.conversationMemberIds(conversation.id)
}
