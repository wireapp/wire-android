package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Composable
import com.wire.android.ui.home.conversations.common.ConversationItemTemplate
import com.wire.android.ui.home.conversations.common.ConversationUserAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversations.common.GroupName
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel

@Composable
fun AllConversationItem(
    generalConversation: GeneralConversation,
    eventType: EventType? = null,
    onConversationItemClick: () -> Unit
) {
    when (val conversationType = generalConversation.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                ConversationItemTemplate(
                    leadingIcon = {
                        GroupConversationAvatar(colorValue = groupColorValue)
                    },
                    title = { GroupName(name = groupName) },
                    eventType = eventType,
                    onConversationItemClick = onConversationItemClick
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            ConversationItemTemplate(
                leadingIcon = {
                    ConversationUserAvatar("")
                },
                title = { UserLabel(userInfoLabel = conversationType.toUserInfoLabel()) },
                eventType = eventType,
                onConversationItemClick = onConversationItemClick
            )
        }
    }
}

