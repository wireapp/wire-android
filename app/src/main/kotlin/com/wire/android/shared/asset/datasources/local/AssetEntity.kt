package com.wire.android.shared.asset.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "size") val size: String,
    @ColumnInfo(name = "type") val type: String
)
