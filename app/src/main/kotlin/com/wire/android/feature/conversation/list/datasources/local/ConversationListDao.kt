package com.wire.android.feature.conversation.list.datasources.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ConversationListDao {

    @Transaction
    @Query("SELECT * FROM conversation")
    suspend fun allConversationListItems(): List<ConversationListItemEntity>

    @Transaction
    @Query("SELECT * FROM conversation WHERE type != :excludeType")
    fun conversationListItemsInBatch(excludeType: Int): PagingSource<Int, ConversationListItemEntity>

    @Transaction
    @Query("SELECT * FROM conversation ORDER BY id LIMIT :count OFFSET :start ")
    suspend fun conversationListItemsInBatch(start: Int, count: Int): List<ConversationListItemEntity>
}
