package com.wire.android.shared.asset.datasources.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AssetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assetEntity: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<AssetEntity>): List<Long>

    @Query("SELECT * FROM asset WHERE id = :id")
    suspend fun assetById(id: Int): AssetEntity?
}
