/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.controlbuttons.JoinButton
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireRadioButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.toUserInfoLabel
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId

@Composable
fun ConversationItemFactory(
    conversation: ConversationItem,
    modifier: Modifier = Modifier,
    isSelectableItem: Boolean = false,
    isChecked: Boolean = false,
    onConversationSelectedOnRadioGroup: () -> Unit = {},
    openConversation: (ConversationItem) -> Unit = {},
    openMenu: (ConversationItem) -> Unit = {},
    openUserProfile: (UserId) -> Unit = {},
    joinCall: (ConversationId) -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit = {}
) {
    val onConversationItemClick = remember(conversation) {
        Clickable(
            enabled = true,
            onClick = {
                when (val lastEvent = conversation.lastMessageContent) {
                    is UILastMessageContent.Connection -> openUserProfile(lastEvent.userId)
                    else -> openConversation(conversation)
                }
            },
            onLongClick = {
                when (conversation.lastMessageContent) {
                    is UILastMessageContent.Connection -> {}
                    else -> openMenu(conversation)
                }
            }
        )
    }
    GeneralConversationItem(
        modifier = modifier,
        conversation = conversation,
        isSelectable = isSelectableItem,
        isChecked = isChecked,
        selectOnRadioGroup = onConversationSelectedOnRadioGroup,
        subTitle = {
            if (!isSelectableItem) {
                when (val messageContent = conversation.lastMessageContent) {
                    is UILastMessageContent.TextMessage -> LastMessageSubtitle(messageContent.messageBody.message)
                    is UILastMessageContent.MultipleMessage -> LastMultipleMessages(messageContent.messages, messageContent.separator)
                    is UILastMessageContent.SenderWithMessage -> LastMessageSubtitleWithAuthor(
                        messageContent.sender,
                        messageContent.message,
                        messageContent.separator
                    )

                    is UILastMessageContent.Connection -> ConnectionLabel(connectionInfo = messageContent)
                    is UILastMessageContent.VerificationChanged -> LastMessageSubtitle(UIText.StringResource(messageContent.textResId))

                    else -> {}
                }
            }
        },
        onConversationItemClick = onConversationItemClick,
        onJoinCallClick = {
            joinCall(conversation.conversationId)
        },
        onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied
    )
}

@Suppress("ComplexMethod")
@Composable
private fun GeneralConversationItem(
    conversation: ConversationItem,
    isChecked: Boolean,
    isSelectable: Boolean,
    onConversationItemClick: Clickable,
    onJoinCallClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectOnRadioGroup: () -> Unit = {},
    subTitle: @Composable () -> Unit = {},
    onAudioPermissionPermanentlyDenied: () -> Unit
) {
    when (conversation) {
        is ConversationItem.GroupConversation -> {
            with(conversation) {
                RowItemTemplate(
                    modifier = modifier,
                    leadingIcon = {
                        Row {
                            if (isSelectable) {
                                WireRadioButton(checked = isChecked, onButtonChecked = {
                                    selectOnRadioGroup()
                                })
                            }
                            GroupConversationAvatar(colorsScheme().conversationColor(id = conversationId))
                        }
                    },
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
                        if (!isSelectable) {
                            if (hasOnGoingCall) {
                                JoinButton(
                                    buttonClick = onJoinCallClick,
                                    onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
                                )
                            } else {
                                Row(
                                    modifier = Modifier.padding(horizontal = dimensions().spacing8x),
                                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                                ) {
                                    if (mutedStatus != MutedConversationStatus.AllAllowed) {
                                        MutedConversationBadge()
                                    }
                                    EventBadgeFactory(eventType = conversation.badgeEventType)
                                }
                            }
                        }
                    },
                )
            }
        }

        is ConversationItem.PrivateConversation -> {
            with(conversation) {
                RowItemTemplate(
                    modifier = modifier,
                    leadingIcon = {
                        Row {
                            if (isSelectable) {
                                WireRadioButton(checked = isChecked, onButtonChecked = {
                                    selectOnRadioGroup()
                                })
                            }
                            ConversationUserAvatar(userAvatarData)
                        }
                    },
                    title = {
                        UserLabel(
                            userInfoLabel = toUserInfoLabel(),
                            searchQuery = searchQuery
                        )
                    },
                    subTitle = subTitle,
                    clickable = onConversationItemClick,
                    trailingIcon = {
                        if (!isSelectable) {
                            Row(
                                modifier = Modifier.padding(horizontal = dimensions().spacing8x),
                                horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                            ) {
                                if (mutedStatus != MutedConversationStatus.AllAllowed) {
                                    MutedConversationBadge()
                                }
                                EventBadgeFactory(eventType = conversation.badgeEventType)
                            }
                        }
                    }
                )
            }
        }

        is ConversationItem.ConnectionConversation -> {
            with(conversation) {
                RowItemTemplate(
                    modifier = modifier,
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

@Composable
fun LoadingConversationItem(modifier: Modifier = Modifier) {
    RowItemTemplate(
        modifier = modifier,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .padding(dimensions().avatarClickablePadding)
                    .clip(CircleShape)
                    .shimmerPlaceholder(visible = true)
                    .border(dimensions().avatarBorderWidth, colorsScheme().outline)
                    .size(dimensions().avatarDefaultSize)
            )
        },
        title = {
            Box(
                modifier = Modifier
                    .height(dimensions().spacing16x)
                    .padding(vertical = dimensions().spacing1x)
                    .shimmerPlaceholder(visible = true)
                    .fillMaxWidth(0.75f)
            )
        },
        subTitle = {
            Box(
                modifier = Modifier
                    .padding(top = dimensions().spacing8x)
                    .shimmerPlaceholder(visible = true)
                    .fillMaxWidth(0.5f)
                    .height(dimensions().spacing6x)
            )
        },
        clickable = remember { Clickable(false) },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewLoadingConversationItem() = WireTheme {
    LoadingConversationItem()
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationItemWithUnreadCount() = WireTheme {
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
            teamId = null,
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationItemWithNoBadges() = WireTheme {
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
            teamId = null,
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationItemWithLastDeletedMessage() = WireTheme {
    ConversationItemFactory(
        conversation = ConversationItem.GroupConversation(
            "groupName looooooooooooooooooooooooooooooooooooong",
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastMessageContent = UILastMessageContent.SenderWithMessage(
                "John".toUIText(),
                UIText.StringResource(R.string.deleted_message_text),
                ":${MarkdownConstants.NON_BREAKING_SPACE}"
            ),
            badgeEventType = BadgeEventType.None,
            selfMemberRole = null,
            teamId = null,
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationItemWithMutedBadgeAndUnreadMentionBadge() = WireTheme {
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
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationItemWithOngoingCall() = WireTheme {
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
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConnectionConversationItemWithReceivedConnectionRequestBadge() = WireTheme {
    ConversationItemFactory(
        conversation = ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.ReceivedConnectionRequest,
            conversationInfo = ConversationInfo("Name")
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConnectionConversationItemWithSentConnectRequestBadge() = WireTheme {
    ConversationItemFactory(
        conversation = ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.SentConnectRequest,
            conversationInfo = ConversationInfo("Name")
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewConnectionConversationItemWithSentConnectRequestBadgeWithUnknownSender() = WireTheme {
    ConversationItemFactory(
        conversation = ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(),
            conversationId = QualifiedID("value", "domain"),
            mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
            lastMessageContent = null,
            badgeEventType = BadgeEventType.SentConnectRequest,
            conversationInfo = ConversationInfo("", isSenderUnavailable = true)
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewPrivateConversationItemWithBlockedBadge() = WireTheme {
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
            userId = UserId("value", "domain"),
            isArchived = false,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED
        ),
        modifier = Modifier,
        isSelectableItem = false,
        isChecked = false,
        {}, {}, {}, {}, {}, {}
    )
}
