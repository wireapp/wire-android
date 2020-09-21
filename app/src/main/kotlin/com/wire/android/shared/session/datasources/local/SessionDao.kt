package com.wire.android.shared.session.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM session WHERE is_current = 1 LIMIT 1")
    suspend fun currentSession(): SessionEntity?

    @Query("SELECT * FROM session")
    suspend fun sessions(): List<SessionEntity>
}
