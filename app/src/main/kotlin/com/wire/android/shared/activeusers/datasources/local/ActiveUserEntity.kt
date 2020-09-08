package com.wire.android.shared.activeusers.datasources.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ActiveUsers")
data class ActiveUserEntity(@PrimaryKey val userId: String) //TODO: add other fields as we need
