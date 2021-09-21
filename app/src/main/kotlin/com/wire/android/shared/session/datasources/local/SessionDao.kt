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

    @Query("UPDATE session SET client_id = :clientId WHERE user_id = :userId")
    suspend fun setClientIdToUserSession(userId: String, clientId: String)

    @Query("SELECT * FROM session WHERE user_id = :userId")
    suspend fun userSession(userId: String): SessionEntity?

    @Query("SELECT * FROM session")
    suspend fun sessions(): List<SessionEntity>

    @Query("UPDATE session SET is_current = 0 WHERE is_current = 1")
    suspend fun setCurrentSessionToDormant()

    @Query("SELECT EXISTS(SELECT 1 FROM session WHERE is_current = 1)")
    suspend fun doesCurrentSessionExist(): Boolean

    @Query("UPDATE session SET is_current = 1 WHERE user_id = :userId")
    suspend fun setSessionCurrent(userId: String)
}
