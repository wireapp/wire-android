package com.wire.android.ui.home.conversationslist.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.calling.controlButtons.JoinButton
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
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    eventType: EventType? = null,
    openConversation: (ConversationId) -> Unit,
    openMenu: (ConversationItem) -> Unit,
    openUserProfile: (UserId) -> Unit,
    openNotificationsOptions: (ConversationItem) -> Unit,
    joinCall: (ConversationId) -> Unit,
) {
    val onConversationItemClick = remember(conversation) {
        Clickable(
            enabled = true,
            onClick = {
                when (val lastEvent = conversation.lastEvent) {
                    is ConversationLastEvent.Connection -> openUserProfile(lastEvent.userId)
                    else -> openConversation(conversation.conversationId)
                }
            },
            onLongClick = {
                when (conversation.lastEvent) {
                    is ConversationLastEvent.Connection -> {
                    }
                    else -> openMenu(conversation)
                }
            }
        )
    }
    GeneralConversationItem(
        conversation = conversation,
        eventType = eventType,
        subTitle = {
            when (val lastEvent = conversation.lastEvent) {
                is ConversationLastEvent.Call -> CallLabel(callInfo = lastEvent)
                is ConversationLastEvent.Mention -> MentionLabel(mentionMessage = lastEvent.mentionMessage)
                is ConversationLastEvent.Connection -> ConnectionLabel(lastEvent)
                is ConversationLastEvent.None -> {
                }
            }
        },
        onConversationItemClick = onConversationItemClick,
        onMutedIconClick = {
            openNotificationsOptions(conversation)
        },
        onJoinCallClick = {
            joinCall(conversation.conversationId)
        }
    )
}

@Composable
private fun GeneralConversationItem(
    conversation: ConversationItem,
    eventType: EventType? = null,
    subTitle: @Composable () -> Unit = {},
    onConversationItemClick: Clickable,
    onMutedIconClick: () -> Unit,
    onJoinCallClick: () -> Unit
) {
    when (conversation) {
        is ConversationItem.GroupConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { GroupConversationAvatar(colorsScheme().conversationColor(id = conversationId)) },
                    title = {
                        ConversationTitle(
                            name = groupName.ifEmpty { stringResource(id = R.string.member_name_deleted_label) },
                            isLegalHold = conversation.isLegalHold
                        )
                    },
                    subTitle = subTitle,
                    eventType = eventType,
                    clickable = onConversationItemClick,
                    trailingIcon = {
                        if (hasOnGoingCall)
                            JoinButton(buttonClick = onJoinCallClick)
                        else if (mutedStatus != MutedConversationStatus.AllAllowed) {
                            MutedConversationBadge(onMutedIconClick)
                        }
                    },
                )
            }
        }
        is ConversationItem.PrivateConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { ConversationUserAvatar(userAvatarData) },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = subTitle,
                    eventType = parsePrivateConversationEventType(connectionState, eventType),
                    clickable = onConversationItemClick,
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
                    leadingIcon = { ConversationUserAvatar(userAvatarData) },
                    title = { UserLabel(userInfoLabel = toUserInfoLabel()) },
                    subTitle = subTitle,
                    eventType = parseConnectionEventType(connectionState),
                    clickable = onConversationItemClick
                )
            }
        }
    }
}

private fun parseConnectionEventType(connectionState: ConnectionState) =
    if (connectionState == ConnectionState.SENT) EventType.SentConnectRequest else EventType.ReceivedConnectionRequest

private fun parsePrivateConversationEventType(connectionState: ConnectionState, eventType: EventType?) =
    if (connectionState == ConnectionState.BLOCKED) EventType.Blocked
    else eventType
