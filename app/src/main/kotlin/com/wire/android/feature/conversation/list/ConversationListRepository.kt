package com.wire.android.feature.conversation.list

import androidx.paging.DataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem

interface ConversationListRepository {
    fun conversationListDataSourceFactory(): DataSource.Factory<Int, ConversationListItem>
}
