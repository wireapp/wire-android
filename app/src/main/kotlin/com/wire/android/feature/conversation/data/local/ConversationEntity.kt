package com.wire.android.feature.conversation.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "conversation",
    primaryKeys = ["id", "domain"],
    indices = [Index(value = ["id"], unique = true)]
)
data class ConversationEntity(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "type") val type: Int
)
