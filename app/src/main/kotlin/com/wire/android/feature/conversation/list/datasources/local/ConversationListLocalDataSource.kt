package com.wire.android.feature.conversation.list.datasources.local

import androidx.paging.DataSource

class ConversationListLocalDataSource(private val conversationListDao: ConversationListDao) {

    fun conversationListDataSourceFactory(): DataSource.Factory<Int, ConversationListItemEntity> =
        conversationListDao.conversationListItemsInBatch()
}
