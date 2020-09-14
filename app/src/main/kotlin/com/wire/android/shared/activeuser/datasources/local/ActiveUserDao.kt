package com.wire.android.shared.activeuser.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActiveUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActiveUserEntity)

    @Query("SELECT * from active_user")
    suspend fun activeUsers(): List<ActiveUserEntity>
}
