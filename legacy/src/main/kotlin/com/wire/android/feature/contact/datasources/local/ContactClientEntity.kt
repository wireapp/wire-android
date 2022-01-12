package com.wire.android.feature.contact.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "contact_client", primaryKeys = ["user_id", "id"],
        foreignKeys = [ForeignKey(entity = ContactEntity::class, parentColumns = ["id"], childColumns = ["user_id"])])
data class ContactClientEntity(
        @ColumnInfo(name = "user_id") val userId: String,
        @ColumnInfo(name = "id") val id: String,
)
