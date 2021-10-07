package com.wire.android.feature.contact.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "contact")
data class ContactEntity(
        @PrimaryKey @ColumnInfo(name = "id") val id: String,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "asset_key") val assetKey: String?
)

data class ContactWithClients(
        @Embedded val contact: ContactEntity,
        @Relation(
                parentColumn = "id",
                entityColumn = "user_id"
        )
        val clients: List<ContactClientEntity>
)
