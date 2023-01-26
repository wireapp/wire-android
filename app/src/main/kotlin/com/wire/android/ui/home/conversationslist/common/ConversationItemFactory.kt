/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.controlbuttons.JoinButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    searchQuery: String,
    openConversation: (ConversationId) -> Unit,
    openMenu: (ConversationItem) -> Unit,
    openUserProfile: (UserId) -> Unit,
    openNotificationsOptions: (ConversationItem) -> Unit,
    joinCall: (ConversationId) -> Unit
) {
    val onConversationItemClick = remember(conversation) {
        Clickable(
            enabled = true,
            onClick = {
                when (val lastEvent = conversation.lastMessageContent) {
                    is UILastMessageContent.Connection -> openUserProfile(lastEvent.userId)
                    else -> openConversation(conversation.conversationId)
                }
            },
            onLongClick = {
                when (conversation.lastMessageContent) {
                    is UILastMessageContent.Connection -> {
                    }

                    else -> openMenu(conversation)
                }
            }
        )
    }
    GeneralConversationItem(
        conversation = conversation,
        searchQuery = searchQuery,
        subTitle = {
            when (val messageContent = conversation.lastMessageContent) {
                is UILastMessageContent.TextMessage -> LastMessageSubtitle(messageContent.messageBody.message)
                is UILastMessageContent.MultipleMessage -> LastMultipleMessages(messageContent.messages, messageContent.separator)
                is UILastMessageContent.SenderWithMessage -> LastMessageSubtitleWithAuthor(
                    messageContent.sender,
                    messageContent.message,
                    messageContent.separator
                )
                is UILastMessageContent.Connection -> ConnectionLabel(connectionInfo = messageContent)
                else -> {}
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
    searchQuery: String,
    conversation: ConversationItem,
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
                            isLegalHold = conversation.isLegalHold,
                            searchQuery = searchQuery
                        )
                    },
                    subTitle = subTitle,
                    clickable = onConversationItemClick,
                    trailingIcon = {
                        if (hasOnGoingCall) {
                            JoinButton(buttonClick = onJoinCallClick)
                        } else {
                            Row(
                                modifier = Modifier.padding(horizontal = dimensions().spacing8x),
                                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                            ) {
                                if (mutedStatus != MutedConversationStatus.AllAllowed) {
                                    MutedConversationBadge(onMutedIconClick)
                                }
                                EventBadgeFactory(eventType = conversation.badgeEventType)
                            }
                        }
                    },
                )
            }
        }

        is ConversationItem.PrivateConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { ConversationUserAvatar(userAvatarData) },
                    title = {
                        UserLabel(
                            userInfoLabel = toUserInfoLabel(),
                            searchQuery = searchQuery
                        )
                    },
                    subTitle = subTitle,
                    clickable = onConversationItemClick,
                    trailingIcon = {
                        Row(
                            modifier = Modifier.padding(horizontal = dimensions().spacing8x),
                            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                        ) {
                            if (mutedStatus != MutedConversationStatus.AllAllowed) {
                                MutedConversationBadge(onMutedIconClick)
                            }
                            EventBadgeFactory(eventType = conversation.badgeEventType)
                        }
                    }
                )
            }
        }

        is ConversationItem.ConnectionConversation -> {
            with(conversation) {
                RowItemTemplate(
                    leadingIcon = { ConversationUserAvatar(userAvatarData) },
                    title = {
                        UserLabel(
                            userInfoLabel = toUserInfoLabel(),
                            searchQuery = searchQuery
                        )
                    },
                    subTitle = subTitle,
                    clickable = onConversationItemClick,
                    trailingIcon = {
                        EventBadgeFactory(
                            modifier = Modifier.padding(horizontal = dimensions().spacing8x),
                            eventType = conversation.badgeEventType
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewGroupConversationItemWithUnreadCount() {
    ConversationItemFactory(
        conversation = ConversationItem.GroupConversation(
            "groupName looooooooooooooooooooooooooooooooooooong",
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastMessageContent = UILastMessageContent.TextMessage(
                MessageBody(UIText.DynamicString("Very looooooooooong messageeeeeeeeeeeeeee"))
            ),
            badgeEventType = BadgeEventType.UnreadMessage(100),
            selfMemberRole = null,
            teamId = null
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewGroupConversationItemWithNoBadges() {
    ConversationItemFactory(
        conversation = ConversationItem.GroupConversation(
            "groupName looooooooooooooooooooooooooooooooooooong",
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastMessageContent = UILastMessageContent.TextMessage(
                MessageBody(UIText.DynamicString("Very looooooooooooooooooooooong messageeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"))
            ),
            badgeEventType = BadgeEventType.None,
            selfMemberRole = null,
            teamId = null
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewGroupConversationItemWithMutedBadgeAndUnreadMentionBadge() {
    ConversationItemFactory(
        conversation = ConversationItem.GroupConversation(
            "groupName looooooooooooooooooooooooooooooooooooong",
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = UILastMessageContent.TextMessage(
                MessageBody(UIText.DynamicString("Very looooooooooooooooooooooong messageeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"))
            ),
            badgeEventType = BadgeEventType.UnreadMention,
            selfMemberRole = null,
            teamId = null
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewGroupConversationItemWithOngoingCall() {
    ConversationItemFactory(
        conversation = ConversationItem.GroupConversation(
            "groupName looooooooooooooooooooooooooooooooooooong",
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = UILastMessageContent.TextMessage(
                MessageBody(UIText.DynamicString("Very looooooooooooooooooooooong messageeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"))
            ),
            badgeEventType = BadgeEventType.UnreadMention,
            selfMemberRole = null,
            teamId = null,
            hasOnGoingCall = true,
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewConnectionConversationItemWithReceivedConnectionRequestBadge() {
    ConversationItemFactory(
        conversation = ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.ReceivedConnectionRequest,
            conversationInfo = ConversationInfo("Name")
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewConnectionConversationItemWithSentConnectRequestBadge() {
    ConversationItemFactory(
        conversation = ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.SentConnectRequest,
            conversationInfo = ConversationInfo("Name")
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}

@Preview
@Composable
fun PreviewPrivateConversationItemWithBlockedBadge() {
    ConversationItemFactory(
        conversation = ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.Blocked,
            conversationInfo = ConversationInfo("Name"),
            blockingState = BlockingState.BLOCKED,
            teamId = null,
            userId = UserId("value", "domain")
        ),
        searchQuery = "",
        {}, {}, {}, {}, {}
    )
}
