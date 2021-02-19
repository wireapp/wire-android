package com.wire.android.feature.conversation.list

import androidx.paging.PagedList
import com.wire.android.feature.conversation.ConversationType
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

interface ConversationListRepository {
    fun conversationListInBatch(pageSize: Int, excludeType: ConversationType): Flow<PagedList<ConversationListItem>>
}
