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
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.common.ConversationTitle
import com.wire.android.ui.home.conversationslist.common.ConversationUserAvatar
import com.wire.android.ui.home.conversationslist.common.UserLabel
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun GeneralConversationItem(
    generalConversation: GeneralConversation,
    eventType: EventType? = null,
    onConversationItemClick: (GeneralConversation) -> Unit,
    onConversationItemLongClick: (GeneralConversation) -> Unit
) {
    when (val conversationType = generalConversation.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = { GroupConversationAvatar(color = colorsScheme().conversationColor(id = conversationId)) },
                    title = {
                        ConversationTitle(
                            name = groupName.ifEmpty { stringResource(id = R.string.empty_group_label) },
                            isLegalHold = conversationType.isLegalHold
                        )
                    },
                    eventType = eventType,
                    onRowItemClicked = { onConversationItemClick(generalConversation) },
                    onRowItemLongClicked = { onConversationItemLongClick(generalConversation) },
                    trailingIcon = {
                        if (this.mutedStatus != MutedConversationStatus.AllAllowed) {
                            MutedConversationIconBadge { onConversationItemClick(generalConversation) }
                        }
                    }
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            RowItemTemplate(
                leadingIcon = { with(conversationType.userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
                title = { UserLabel(userInfoLabel = conversationType.toUserInfoLabel()) },
                eventType = eventType,
                onRowItemClicked = { onConversationItemClick(generalConversation) },
                onRowItemLongClicked = { onConversationItemLongClick(generalConversation) }
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
