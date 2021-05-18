package com.wire.android.feature.auth.client.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(client: ClientEntity)
}