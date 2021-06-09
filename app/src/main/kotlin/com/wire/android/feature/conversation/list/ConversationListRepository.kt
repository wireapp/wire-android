package com.wire.android.feature.conversation.list

import androidx.paging.PagingData
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.ConversationType
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

interface ConversationListRepository {
    fun conversationListInBatch(pageSize: Int, excludeType: ConversationType): Flow<PagingData<ConversationListItem>>

    suspend fun conversationListInBatch(start: Int, count: Int): Either<Failure, List<ConversationListItem>>
}
