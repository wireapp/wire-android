package com.wire.android.feature.conversation.list.datasources

import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListItemEntity
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import java.io.File

class ConversationListMapper(
    private val conversationMapper: ConversationMapper,
    private val contactMapper: ContactMapper
) {

    fun fromEntity(listItemEntity: ConversationListItemEntity, profilePictures: List<File?>): ConversationListItem {
        val conversation = conversationMapper.fromEntity(listItemEntity.conversation)
        val contacts = listItemEntity.members.mapIndexed { index, contactEntity ->
            contactMapper.fromContactEntity(contactEntity, profilePictures.getOrNull(index))
        }

        return ConversationListItem(conversation = conversation, members = contacts)
    }
}
