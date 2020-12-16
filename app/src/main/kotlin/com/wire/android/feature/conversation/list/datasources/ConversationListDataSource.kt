package com.wire.android.feature.conversation.list.datasources

import androidx.paging.DataSource
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class ConversationListDataSource(
    private val conversationListLocalDataSource: ConversationListLocalDataSource,
    private val conversationListMapper: ConversationListMapper
) : ConversationListRepository {

    override fun conversationListDataSourceFactory(): DataSource.Factory<Int, ConversationListItem> =
        conversationListLocalDataSource.conversationListDataSourceFactory().map {
            conversationListMapper.fromEntity(it)
        }
}
