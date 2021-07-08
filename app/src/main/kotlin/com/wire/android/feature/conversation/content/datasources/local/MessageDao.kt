package com.wire.android.feature.conversation.content.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * from message where conversation_id = :conversationId")
    fun messagesByConversationId(conversationId: String): Flow<List<MessageAndContactEntity>>
}
