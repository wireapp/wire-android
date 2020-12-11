package com.wire.android.feature.conversation.members.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationMembersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(conversationMember: ConversationMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversationMemberList: List<ConversationMemberEntity>)

    @Query("SELECT * FROM conversation_member")
    suspend fun allConversationMembers(): List<ConversationMemberEntity>

    @Query("SELECT contact_id FROM conversation_member WHERE conversation_id = :conversationId")
    suspend fun conversationMembers(conversationId: String): List<String>
}
