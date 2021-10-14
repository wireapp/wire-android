package com.wire.android.feature.contact.datasources.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactClientEntity>)

    @Query("SELECT * FROM contact_client")
    suspend fun clients(): List<ContactClientEntity>

    @Query("SELECT * FROM contact_client WHERE user_id IN (:userIds)")
    suspend fun clientsByUserId(userIds: Set<String>): List<ContactClientEntity>

    @Delete
    fun delete(contactClientEntity: ContactClientEntity)
}
