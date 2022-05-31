package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.home.conversationslist.CallLabel
import com.wire.android.ui.home.conversationslist.ConnectionLabel
import com.wire.android.ui.home.conversationslist.MentionLabel
import com.wire.android.ui.home.conversationslist.MutedConversationBadge
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    eventType: EventType? = null,
    openConversation: (ConversationId) -> Unit,
    openMenu: (ConversationItem) -> Unit,
    openUserProfile: (UserId) -> Unit,
) {
    GeneralConversationItem(
        conversation = conversation,
        eventType = eventType,
        subTitle = {
            when (val lastEvent = conversation.lastEvent) {
                is ConversationLastEvent.Call -> CallLabel(callInfo = lastEvent)
                is ConversationLastEvent.Mention -> MentionLabel(mentionMessage = lastEvent.mentionMessage)
                is ConversationLastEvent.Connection -> ConnectionLabel(lastEvent)
                is ConversationLastEvent.None -> {}
            }
        },
        onConversationItemClick = {
            when (val lastEvent = conversation.lastEvent) {
                is ConversationLastEvent.Connection -> openUserProfile(lastEvent.userId)
                else -> openConversation(conversation.conversationId)
            }
        },
        onConversationItemLongClick = {
            when (conversation.lastEvent) {
                is ConversationLastEvent.Connection -> {}
                else -> openMenu(conversation)
            }
        },
        onMutedIconClick = {
           // openNotificationsOptions(conversation)
        },
    )
}

@Composable
private fun GeneralConversationItem(
    conversation: ConversationItem,
    eventType: EventType? = null,
    subTitle: @Composable () -> Unit = {},
    onConversationItemClick: () -> Unit,
    onConversationItemLongClick: () -> Unit,
    onMutedIconClick: () -> Unit,
) {
    when (conversation) {
        is ConversationItem.GroupConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { GroupConversationAvatar(colorsScheme().conversationColor(id = conversationId)) },
                    title = { ConversationTitle(name = groupName, isLegalHold = conversation.isLegalHold) },
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
        is ConversationItem.PrivateConversation -> {
            with(conversation) {
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
        is ConversationItem.ConnectionConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { with(userInfo) { ConversationUserAvatar(avatarAsset, availabilityStatus) } },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = subTitle,
                    eventType = eventType,
                    onRowItemClicked = onConversationItemClick,
                    onRowItemLongClicked = onConversationItemLongClick,
                )
            }
        }
    }
}
