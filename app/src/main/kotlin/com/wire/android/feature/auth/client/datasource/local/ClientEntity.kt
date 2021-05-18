package com.wire.android.feature.auth.client.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client")
data class ClientEntity (@PrimaryKey @ColumnInfo(name = "id") val id : String)
