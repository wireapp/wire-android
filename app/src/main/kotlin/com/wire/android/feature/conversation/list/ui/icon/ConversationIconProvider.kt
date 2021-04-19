package com.wire.android.feature.conversation.list.ui.icon

import com.wire.android.feature.conversation.list.ui.ConversationListItem
import com.wire.android.shared.asset.ui.imageloader.IconLoader

class ConversationIconProvider(private val iconLoader: IconLoader) {

    fun provide(conversationListItem: ConversationListItem): ConversationIcon =
        when {
            conversationListItem.members.isEmpty() -> NoParticipantsConversationIcon()

            conversationListItem.members.size == 1 -> SingleParticipantConversationIcon(conversationListItem.members[0], iconLoader)

            else -> {
                val contacts = conversationListItem.members
                    .sortedBy { it.id }
                    .take(GroupConversationIcon.MAX_ICON_COUNT)

                GroupConversationIcon(contacts, iconLoader)
            }
        }
}
