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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.rememberSelfDeletionTimer
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.PersistentMap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageContainerItem(
    message: UIMessage,
    conversationDetailsData: ConversationDetailsData,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    audioMessagesState: PersistentMap<String, AudioState>,
    assetStatus: AssetTransferStatus? = null,
    onLongClicked: (UIMessage.Regular) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    isContentClickable: Boolean = false,
    onMessageClick: (messageId: String) -> Unit = {},
    defaultBackgroundColor: Color = Color.Transparent,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    onReplyClickable: Clickable? = null,
    isSelectedMessage: Boolean = false,
    isInteractionAvailable: Boolean = true
) {
    val selfDeletionTimerState = rememberSelfDeletionTimer(message.header.messageStatus.expirationStatus)
    if (
        selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable &&
        !message.isPending &&
        !message.sendingFailed
    ) {
        selfDeletionTimerState.startDeletionTimer(
            message = message,
            assetTransferStatus = assetStatus,
            onStartMessageSelfDeletion = onSelfDeletingMessageRead
        )
    }
    // padding needed to have same top padding for avatar and rest composables in message item
    val fullAvatarOuterPadding = dimensions().avatarClickablePadding + dimensions().avatarStatusBorderSize

    val colorAnimation = remember { Animatable(Color.Transparent) }
    val highlightColor = colorsScheme().primaryVariant
    val transparentColor = colorsScheme().primary.copy(alpha = 0F)
    LaunchedEffect(isSelectedMessage) {
        if (isSelectedMessage) {
            colorAnimation.snapTo(highlightColor)
            colorAnimation.animateTo(
                transparentColor,
                tween(SELECTED_MESSAGE_ANIMATION_DURATION)
            )
        }
    }

    Row(
        Modifier
            .customizeMessageBackground(
                defaultBackgroundColor,
                message.sendingFailed, message.decryptionFailed,
                message.header.messageStatus.isDeleted,
                isSelectedMessage,
                selfDeletionTimerState,
                colorAnimation.value
            )
            .then(
                when (message) {
                    is UIMessage.Regular -> Modifier.combinedClickable(enabled = true, onClick = {
                        if (isContentClickable) {
                            onMessageClick(message.header.messageId)
                        }
                    },
                        onLongClick = remember(message) {
                            {
                                if (!isContentClickable && !message.header.messageStatus.isDeleted) {
                                    onLongClicked(message)
                                }
                            }
                        }
                    )

                    is UIMessage.System -> Modifier
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    end = dimensions().messageItemHorizontalPadding,
                    top = if (showAuthor) dimensions().spacing0x else dimensions().spacing4x,
                    bottom = if (useSmallBottomPadding) dimensions().spacing2x else dimensions().messageItemBottomPadding
                )
        ) {
            Box(Modifier.width(dimensions().spacing56x), contentAlignment = Alignment.TopEnd) {
                when (message) {
                    is UIMessage.Regular -> RegularMessageItemLeading(
                        header = message.header,
                        showAuthor = showAuthor,
                        userAvatarData = message.userAvatarData,
                        isContentClickable = isContentClickable,
                        onOpenProfile = onOpenProfile
                    )

                    is UIMessage.System -> SystemMessageItemLeading(
                        modifier = Modifier.padding(end = fullAvatarOuterPadding),
                        messageContent = message.messageContent
                    )
                }
            }
            Spacer(Modifier.width(dimensions().messageItemHorizontalPadding - fullAvatarOuterPadding))
            Box(Modifier.weight(1F)) {
                when (message) {
                    is UIMessage.Regular -> RegularMessageItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        showAuthor = showAuthor,
                        audioMessagesState = audioMessagesState,
                        assetStatus = assetStatus,
                        onAudioClick = onAudioClick,
                        onChangeAudioPosition = onChangeAudioPosition,
                        onLongClicked = onLongClicked,
                        onAssetMessageClicked = onAssetMessageClicked,
                        onImageMessageClicked = onImageMessageClicked,
                        onOpenProfile = onOpenProfile,
                        onReactionClicked = onReactionClicked,
                        onResetSessionClicked = onResetSessionClicked,
                        onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                        onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                        onLinkClick = onLinkClick,
                        onReplyClickable = onReplyClickable,
                        isInteractionAvailable = isInteractionAvailable,
                        searchQuery = searchQuery,
                        shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                        shouldDisplayFooter = shouldDisplayFooter
                    )

                    is UIMessage.System -> SystemMessageItem(
                        message = message,
                        onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                        onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                        isInteractionAvailable = isInteractionAvailable,
                    )
                }
            }
        }
    }
    if (message.messageContent is UIMessageContent.SystemMessage.ConversationMessageCreated) {
        Row(
            Modifier
                .background(colorsScheme().background)
                .height(dimensions().spacing24x)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(start = dimensions().spacing56x)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.wireTypography.title03,
                text = (message.messageContent as UIMessageContent.SystemMessage.ConversationMessageCreated).date
            )
        }
    }
}

private const val SELECTED_MESSAGE_ANIMATION_DURATION = 2000
