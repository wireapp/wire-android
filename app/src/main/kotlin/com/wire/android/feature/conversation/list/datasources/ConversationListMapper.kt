package com.wire.android.feature.conversation.list.datasources

import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListItemEntity
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class ConversationListMapper(
    private val conversationMapper: ConversationMapper,
    private val contactMapper: ContactMapper
) {

    fun fromEntity(listItemEntity: ConversationListItemEntity): ConversationListItem {
        val conversation = conversationMapper.fromEntity(listItemEntity.conversation)
        val contacts = contactMapper.fromContactEntityList(listItemEntity.members)

        return ConversationListItem(conversation = conversation, members = contacts)
    }
}
