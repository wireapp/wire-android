package com.wire.android.feature.conversation.content.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * from message where conversation_id = :conversationId")
    fun messagesByConversationId(conversationId: String): Flow<List<CombinedMessageContactEntity>>

    @Query("SELECT * FROM message where conversation_id = :conversationId AND is_read = 0 ORDER BY time DESC LIMIT :size")
    suspend fun latestUnreadMessagesByConversationId(conversationId: String, size: Int): List<CombinedMessageContactEntity>
}
