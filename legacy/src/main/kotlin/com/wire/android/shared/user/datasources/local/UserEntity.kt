package com.wire.android.shared.user.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
        @PrimaryKey @ColumnInfo(name = "id") val id: String,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "email") val email: String? = null,
        @ColumnInfo(name = "username") val username: String? = null,
        @ColumnInfo(name = "asset_key") val assetKey: String? = null
)
