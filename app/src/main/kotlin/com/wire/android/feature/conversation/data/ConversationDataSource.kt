package com.wire.android.feature.conversation.data

import androidx.paging.DataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.local.ConversationLocalDataSource
import com.wire.android.feature.conversation.data.remote.ConversationRemoteDataSource

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationRemoteDataSource: ConversationRemoteDataSource,
    private val conversationLocalDataSource: ConversationLocalDataSource
) : ConversationsRepository {

    override fun conversationsDataFactory(): DataSource.Factory<Int, Conversation> =
        conversationLocalDataSource.conversationsDataFactory().map {
            conversationMapper.fromEntity(it)
        }

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
