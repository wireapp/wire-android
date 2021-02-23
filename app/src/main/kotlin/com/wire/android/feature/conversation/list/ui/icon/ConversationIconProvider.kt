package com.wire.android.feature.conversation.list.ui.icon

import com.wire.android.feature.contact.ui.icon.ContactIconLoader
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class ConversationIconProvider(private val contactIconLoader: ContactIconLoader) {

    fun provide(conversationListItem: ConversationListItem): ConversationIcon =
        when {
            conversationListItem.members.isEmpty() -> NoParticipantsConversationIcon()

            conversationListItem.members.size == 1 -> SingleParticipantConversationIcon(conversationListItem.members[0], contactIconLoader)

            else -> {
                val contacts = conversationListItem.members
                    .sortedBy { it.id }
                    .take(GroupConversationIcon.MAX_ICON_COUNT)

                GroupConversationIcon(contacts, contactIconLoader)
            }
        }
}
