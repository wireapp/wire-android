package com.wire.android.feature.conversation.data

import androidx.paging.DataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation

interface ConversationsRepository {
    fun conversationsDataFactory(): DataSource.Factory<Int, Conversation>

    suspend fun conversationsByBatch(start: String, size: Int, ids: List<String>): Either<Failure, List<Conversation>>
}
