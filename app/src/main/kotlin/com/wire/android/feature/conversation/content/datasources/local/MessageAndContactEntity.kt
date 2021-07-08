package com.wire.android.feature.conversation.content.datasources.local

import androidx.room.Embedded
import androidx.room.Relation
import com.wire.android.feature.contact.datasources.local.ContactEntity

class MessageAndContactEntity(
    @Embedded
    val messageEntity: MessageEntity,
    @Relation(
        parentColumn = "sender_user_id",
        entityColumn = "id"
    )
    val contactEntity: ContactEntity
)
