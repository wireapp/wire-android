package com.wire.android.ui.home.conversations.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.home.conversationslist.AllConversationItem
import com.wire.android.ui.home.conversationslist.CallConversationItem
import com.wire.android.ui.home.conversationslist.common.EventBadgeFactory
import com.wire.android.ui.home.conversationslist.common.RowItem
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
        is GeneralConversation -> {
            AllConversationItem(
                item,
                eventType,
                onConversationItemClick
            )
        }
        is Call -> {
            CallConversationItem(
                item,
                eventType,
                onConversationItemClick
            )
        }
        is Mention -> {

        }
    }
}

