package com.wire.android.feature.conversation.data

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation

interface ConversationsRepository {
    fun conversationsByBatch(
        pagingDelegate: ConversationsPagingDelegate
    ): LiveData<Either<Failure, PagedList<Conversation>>>
}
