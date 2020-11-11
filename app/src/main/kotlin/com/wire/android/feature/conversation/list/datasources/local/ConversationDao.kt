package com.wire.android.feature.conversation.list.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(user: List<ConversationEntity>)

    @Query("SELECT * FROM conversation")
    suspend fun conversations(): List<ConversationEntity>
}
