package com.wire.android.feature.conversation.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "type") val type: Int
)
