package com.wire.android.feature.conversation.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversationList: List<ConversationEntity>)

    @Update
    suspend fun updateConversations(conversationList: List<ConversationEntity>)

    @Query("DELETE FROM conversation WHERE id = :id")
    fun deleteConversationById(id: String)

    @Query("SELECT * FROM conversation")
    suspend fun conversations(): List<ConversationEntity>

    @Query("SELECT COUNT(*) FROM conversation")
    suspend fun count(): Int

    @Delete
    fun delete(conversation: ConversationEntity)
}
