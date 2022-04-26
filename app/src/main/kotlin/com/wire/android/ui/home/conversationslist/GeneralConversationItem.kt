package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.common.ConversationUserAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversations.common.GroupName
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.main.conversationlist.common.UserLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

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
                    onRowItemLongClicked = onConversationItemLongClick,
                    trailingIcon = {
                        if (this.mutedStatus != MutedConversationStatus.AllAllowed) {
                            MutedConversationIconBadge(onConversationItemClick)
                        }
                    }
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

@Composable
private fun MutedConversationIconBadge(onConversationItemClick: () -> Unit) {
    WireSecondaryButton(
        onClick = onConversationItemClick,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_mute),
                contentDescription = stringResource(R.string.content_description_mute),
                modifier = Modifier.size(dimensions().spacing16x)
            )
        },
        fillMaxWidth = false,
        minHeight = dimensions().badgeSmallMinSize.height,
        minWidth = dimensions().badgeSmallMinSize.width,
        shape = RoundedCornerShape(size = dimensions().spacing6x),
        contentPadding = PaddingValues(0.dp),
    )
}
