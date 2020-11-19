package com.wire.android.feature.conversation.data

import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationsRepository {
    fun conversationsByBatch(
        pagingDelegate: ConversationsPagingDelegate
    ): Flow<Either<Failure, PagedList<Conversation>>>
}
