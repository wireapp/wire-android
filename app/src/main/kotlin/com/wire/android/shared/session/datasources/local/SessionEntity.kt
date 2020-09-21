package com.wire.android.shared.session.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wire.android.shared.user.datasources.local.UserEntity

@Entity(
    tableName = "session",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class, parentColumns = arrayOf("id"), childColumns = arrayOf("user_id"), onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["user_id"], unique = true)]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "access_token") val accessToken: String,
    @ColumnInfo(name = "token_type") val tokenType: String,
    @ColumnInfo(name = "refresh_token") val refreshToken: String,
    @ColumnInfo(name = "is_current") val isCurrent: Boolean
)
