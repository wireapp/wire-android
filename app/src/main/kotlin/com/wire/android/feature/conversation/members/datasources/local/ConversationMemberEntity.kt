package com.wire.android.feature.conversation.members.datasources.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.wire.android.feature.conversation.data.local.ConversationEntity

@Entity(
    tableName = "conversation_member",
    primaryKeys = ["contact_id", "conversation_id"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = arrayOf("id"), childColumns = arrayOf("conversation_id"), onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(name="conversation_members_conversation_id", value = ["conversation_id"])
    ]
)
data class ConversationMemberEntity(
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "conversation_domain") val conversationDomain: String,
    @ColumnInfo(name = "contact_id") val contactId: String
)
