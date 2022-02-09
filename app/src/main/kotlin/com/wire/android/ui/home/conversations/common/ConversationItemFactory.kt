package com.wire.android.ui.home.conversations.common

import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversationslist.CallConversationItem
import com.wire.android.ui.home.conversationslist.GeneralConversationItem
import com.wire.android.ui.home.conversationslist.MentionConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    eventType: EventType? = null,
    onConversationItemClick: () -> Unit,
    onConversationItemLongClick: () -> Unit,
) {
    when (conversation) {
        is ConversationMissedCall -> CallConversationItem(
            conversation,
            eventType,
            onConversationItemClick,
            onConversationItemLongClick
        )
        is ConversationUnreadMention -> MentionConversationItem(
            conversation,
            eventType,
            onConversationItemClick,
            onConversationItemLongClick
        )
        is GeneralConversation -> GeneralConversationItem(
            conversation,
            eventType,
            onConversationItemClick,
            onConversationItemLongClick
        )
    }
}

