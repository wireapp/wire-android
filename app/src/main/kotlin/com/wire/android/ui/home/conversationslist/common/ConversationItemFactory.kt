package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.home.conversationslist.CallLabel
import com.wire.android.ui.home.conversationslist.ConnectionLabel
import com.wire.android.ui.home.conversationslist.MentionLabel
import com.wire.android.ui.home.conversationslist.MutedConversationBadge
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationMissedCall
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.PendingConnectionItem
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    eventType: EventType? = null,
    openConversation: () -> Unit,
    onConversationItemLongClick: () -> Unit,
    openUserProfile: (UserId) -> Unit,
    onMutedIconClick: () -> Unit,
) {
    MainConversationItem(
        conversation = conversation,
        eventType = eventType,
        subTitle = {
            when (conversation) {
                is ConversationMissedCall -> CallLabel(callInfo = conversation.callInfo)
                is ConversationUnreadMention -> MentionLabel(mentionMessage = conversation.mentionInfo.mentionMessage)
                is GeneralConversation -> null // TODO implement last conversation message
                is PendingConnectionItem -> ConnectionLabel(conversation.connectionInfo)
            }
        },
        onConversationItemClick = {
            when (conversation) {
                is PendingConnectionItem -> openUserProfile(conversation.connectionInfo.userId)
                else -> openConversation()
            }
        },
        onConversationItemLongClick = {
            when (conversation) {
                is PendingConnectionItem -> {}
                else -> onConversationItemLongClick()
            }
        },
        onMutedIconClick = onMutedIconClick,
    )
}

@Composable
private fun MainConversationItem(
    conversation: ConversationItem,
    eventType: EventType? = null,
    subTitle: @Composable () -> Unit = {},
    onConversationItemClick: () -> Unit,
    onConversationItemLongClick: () -> Unit,
    onMutedIconClick: () -> Unit,
) {
    when (val conversationType = conversation.conversationType) {
        is ConversationType.GroupConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = { GroupConversationAvatar(colorValue = groupColorValue) },
                    title = { ConversationTitle(name = groupName, isLegalHold = conversationType.isLegalHold) },
                    subTitle = subTitle,
                    eventType = eventType,
                    onRowItemClicked = onConversationItemClick,
                    onRowItemLongClicked = onConversationItemLongClick,
                    trailingIcon = {
                        if (mutedStatus != MutedConversationStatus.AllAllowed) {
                            MutedConversationBadge(onMutedIconClick)
                        }
                    },
                )
            }
        }
        is ConversationType.PrivateConversation -> {
            with(conversationType) {
                RowItemTemplate(
                    leadingIcon = { with(userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = subTitle,
                    eventType = eventType,
                    onRowItemClicked = onConversationItemClick,
                    onRowItemLongClicked = onConversationItemLongClick,
                    trailingIcon = {
                        if (mutedStatus != MutedConversationStatus.AllAllowed) {
                            MutedConversationBadge(onMutedIconClick)
                        }
                    },
                )
            }
        }
    }
}
