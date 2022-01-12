package com.wire.android.shared.user.datasources.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun userById(userId: String): UserEntity

    @Query("SELECT * FROM user")
    suspend fun users(): List<UserEntity>
}
