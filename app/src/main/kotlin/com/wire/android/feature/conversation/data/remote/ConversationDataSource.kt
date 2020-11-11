package com.wire.android.feature.conversation.data.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationRemoteDataSource: ConversationRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationsRepository {

    override suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, List<Conversation>> =
        suspending {
            conversationRemoteDataSource.conversationsByBatch(start, size, ids).map {
                conversationMapper.fromConversationsResponse(it)
            }.flatMap { conversationList ->
                val entityList = conversationMapper.toEntityList(conversationList)
                conversationLocalDataSource.saveConversations(entityList).map { conversationList }
            }
        }
}
