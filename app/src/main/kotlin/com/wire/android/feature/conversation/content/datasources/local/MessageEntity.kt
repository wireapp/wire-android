package com.wire.android.feature.conversation.content.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.wire.android.feature.conversation.data.local.ConversationEntity

@Entity(
    tableName = "message",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("conversation_id"),
            onDelete = ForeignKey.CASCADE
        )
    ],
)
data class MessageEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "time") val time: String
)
