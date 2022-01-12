package com.wire.android.feature.contact.datasources.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contact WHERE id IN(:ids)")
    suspend fun contactsById(ids: Set<String>): List<ContactEntity>

    @Query("SELECT * FROM contact")
    suspend fun contacts(): List<ContactEntity>

    @Query("SELECT * FROM contact")
    suspend fun contactsWithClients(): List<ContactWithClients>

    @Query("SELECT * FROM contact WHERE id IN(:ids)")
    suspend fun contactsByIdWithClients(ids: Set<String>): List<ContactWithClients>

    @Delete
    fun delete(contactEntity: ContactEntity)
}
