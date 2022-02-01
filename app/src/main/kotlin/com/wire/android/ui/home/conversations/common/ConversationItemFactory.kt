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


private val factoryMap = mapOf<Conversation, @Composable () -> Unit>()

@Composable
inline fun <reified Conversation> ConversationItemFactory(
    item: Conversation,
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

