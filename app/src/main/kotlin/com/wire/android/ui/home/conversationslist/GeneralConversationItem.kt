package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.common.ConversationTitle
import com.wire.android.ui.home.conversationslist.common.ConversationUserAvatar
import com.wire.android.ui.home.conversationslist.common.UserLabel
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel

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
                    title = { ConversationTitle(name = groupName, isLegalHold = conversationType.isLegalHold) },
                    eventType = eventType,
                    onRowItemClicked = onConversationItemClick,
                    onRowItemLongClicked = onConversationItemLongClick
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            RowItemTemplate(
                leadingIcon = { with(conversationType.userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
                title = { UserLabel(userInfoLabel = conversationType.toUserInfoLabel()) },
                eventType = eventType,
                onRowItemClicked = onConversationItemClick,
                onRowItemLongClicked = onConversationItemLongClick
            )
        }
    }
}


