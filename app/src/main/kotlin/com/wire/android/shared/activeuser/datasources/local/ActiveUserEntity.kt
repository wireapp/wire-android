package com.wire.android.shared.activeuser.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_user")
data class ActiveUserEntity(@PrimaryKey @ColumnInfo(name = "user_id") val userId: String) //TODO: add other fields as we need
