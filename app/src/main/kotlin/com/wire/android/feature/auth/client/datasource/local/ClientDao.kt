package com.wire.android.feature.auth.client.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity)

    @Query("SELECT * FROM client")
    suspend fun clients(): List<ClientEntity>
}
