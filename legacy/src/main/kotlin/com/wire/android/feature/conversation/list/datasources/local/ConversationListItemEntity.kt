package com.wire.android.feature.conversation.list.datasources.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity

data class ConversationListItemEntity(
    @Embedded
    val conversation: ConversationEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(ConversationMemberEntity::class, parentColumn = "conversation_id", entityColumn = "contact_id")
    )
    val members: List<ContactEntity>
)
