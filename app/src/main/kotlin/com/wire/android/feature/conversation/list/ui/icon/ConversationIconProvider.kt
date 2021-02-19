package com.wire.android.feature.conversation.list.ui.icon

import com.wire.android.feature.contact.ui.icon.ContactIconProvider
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class ConversationIconProvider(private val contactIconProvider: ContactIconProvider) {

    fun provide(conversationListItem: ConversationListItem): ConversationIcon =
        when {
            conversationListItem.members.isEmpty() -> NoParticipantsConversationIcon()

            conversationListItem.members.size == 1 -> {
                val contactIcon = contactIconProvider.provide(conversationListItem.members[0])
                SingleParticipantConversationIcon(contactIcon)
            }

            else -> {
                val contactIcons = conversationListItem.members
                    .sortedBy { it.id }
                    .take(GroupConversationIcon.MAX_ICON_COUNT)
                    .map { contactIconProvider.provide(it) }

                GroupConversationIcon(contactIcons)
            }
        }
}
