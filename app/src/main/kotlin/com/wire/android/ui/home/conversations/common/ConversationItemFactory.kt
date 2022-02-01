package com.wire.android.ui.home.conversations.common

import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.AllConversationItem
import com.wire.android.ui.home.conversationslist.CallConversationItem
import com.wire.android.ui.home.conversationslist.MentionConversationItem
import com.wire.android.ui.home.conversationslist.model.Call
import com.wire.android.ui.home.conversationslist.model.Conversation
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.Mention

@Composable
inline fun <T : Conversation> ConversationItemFactory(
    item: T,
    eventType: EventType? = null,
    noinline onConversationItemClick: () -> Unit
) {
    when (item) {
        is GeneralConversation -> AllConversationItem(
            item,
            eventType,
            onConversationItemClick
        )
        is Call -> CallConversationItem(
            item,
            eventType,
            onConversationItemClick
        )
        is Mention -> MentionConversationItem(
            item,
            eventType,
            onConversationItemClick
        )
        else -> throw IllegalStateException("Illegal item type for ConversationItemFactory")
    }
}

