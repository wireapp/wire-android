package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.home.conversations.common.ConversationUserAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversations.common.GroupName
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel

@Composable
fun GeneralConversationItem(
    generalConversation: GeneralConversation,
    eventType: EventType? = null,
    onConversationItemClick: () -> Unit,
    onConversationItemLongClick: () -> Unit
) {
    when (val conversationType = generalConversation.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = {
                        GroupConversationAvatar(colorValue = groupColorValue)
                    },
                    title = { GroupName(name = groupName) },
                    eventType = eventType,
                    onRowItemClicked = onConversationItemClick,
                    onRowItemLongClicked = onConversationItemLongClick
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            RowItemTemplate(
                leadingIcon = {
                    ConversationUserAvatar(conversationType.userInfo.avatarAsset)
                },
                title = { UserLabel(userInfoLabel = conversationType.toUserInfoLabel()) },
                eventType = eventType,
                onRowItemClicked = onConversationItemClick,
                onRowItemLongClicked = onConversationItemLongClick
            )
        }
    }
}


