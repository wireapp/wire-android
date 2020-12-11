package com.wire.android.feature.conversation.list.datasources.local

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ConversationListDao {

    @Transaction
    @Query("SELECT * FROM conversation")
    suspend fun allConversationListItems(): List<ConversationListItemEntity>

    @Transaction
    @Query("SELECT * FROM conversation")
    fun conversationListItemsInBatch(): DataSource.Factory<Int, ConversationListItemEntity>
}
