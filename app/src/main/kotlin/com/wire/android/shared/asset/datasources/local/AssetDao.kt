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
    suspend fun insertAll(assetEntity: List<AssetEntity>): List<Long>

    @Query("SELECT * FROM asset WHERE id = :id")
    suspend fun assetById(id: Int): AssetEntity?

    @Query("SELECT download_key FROM asset WHERE id = :id")
    suspend fun downloadKey(id: Int): String?

    @Query("UPDATE asset SET storage_path = :storagePath WHERE id = :id")
    suspend fun updatePath(id: Int, storagePath: String?)
}
