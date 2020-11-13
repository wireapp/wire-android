package com.wire.android.feature.conversation.data.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.feature.conversation.list.usecase.Conversation

class ConversationDataSource(
    private val conversationMapper: ConversationMapper,
    private val conversationsRemoteDataSource: ConversationsRemoteDataSource
) : ConversationsRepository {

    override suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, List<Conversation>> =
        conversationsRemoteDataSource.conversationsByBatch(start, size, ids).map {
            conversationMapper.fromConversationsResponse(it)
        }
}
