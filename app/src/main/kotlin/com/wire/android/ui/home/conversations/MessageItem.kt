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

package com.wire.android.ui.home.conversations

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle
import com.wire.android.ui.home.conversations.messages.QuotedUnavailable
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserId

// TODO: a definite candidate for a refactor and cleanup
@Suppress("ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    showAuthor: Boolean = true,
    audioMessagesState: Map<String, AudioState>,
    onLongClicked: (UIMessage.Regular) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onSelfDeletingMessageRead: (UIMessage.Regular) -> Unit,
    onFailedMessageRetryClicked: (String) -> Unit = {},
    onFailedMessageCancelClicked: (String) -> Unit = {}
) {
    with(message) {
        val selfDeletionTimerState = rememberSelfDeletionTimer(expirationStatus)
        if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            startDeletionTimer(
                message = message,
                expirableTimer = selfDeletionTimerState,
                onStartMessageSelfDeletion = onSelfDeletingMessageRead
            )
        }

        val backgroundColorModifier = if (message.sendingFailed || message.receivingFailed) {
            Modifier.background(colorsScheme().messageErrorBackgroundColor)
        } else if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable && !message.isDeleted) {
            val color by animateColorAsState(
                colorsScheme().primaryVariant.copy(selfDeletionTimerState.alphaBackgroundColor()),
                tween(),
                label = "message background color"
            )

            Modifier.background(color)
        } else {
            Modifier
        }

        Box(backgroundColorModifier) {
            val fullAvatarOuterPadding = if (showAuthor) {
                dimensions().userAvatarClickablePadding + dimensions().userAvatarStatusBorderSize
            } else {
                0.dp
            }
            val halfItemBottomPadding = dimensions().messageItemBottomPadding / 2
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        enabled = message.isAvailable,
                        onClick = { }, // TODO: implement some action onClick
                        onLongClick = remember(message) { { onLongClicked(message) } }
                    )
                    .padding(
                        end = dimensions().spacing16x,
                        top = halfItemBottomPadding - fullAvatarOuterPadding,
                        bottom = halfItemBottomPadding
                    )
            ) {
                Spacer(Modifier.padding(start = dimensions().spacing8x - fullAvatarOuterPadding))

                val isProfileRedirectEnabled =
                    header.userId != null &&
                            !(header.isSenderDeleted || header.isSenderUnavailable)

                if (showAuthor) {
                    val avatarClickable = remember {
                        Clickable(enabled = isProfileRedirectEnabled) {
                            onOpenProfile(header.userId!!.toString())
                        }
                    }
                    UserProfileAvatar(
                        avatarData = message.userAvatarData,
                        clickable = avatarClickable
                    )
                } else {
                    Spacer(Modifier.width(MaterialTheme.wireDimensions.userAvatarDefaultSize))
                }
                Spacer(Modifier.padding(start = dimensions().spacing16x - fullAvatarOuterPadding))
                Column {
                    Spacer(modifier = Modifier.height(fullAvatarOuterPadding))
                    if (showAuthor) {
                        MessageAuthorRow(messageHeader = message.header)
                    }
                    if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
                        MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted())

                        // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
                        // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
                        // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
                        if (isDeleted) {
                            EphemeralMessageExpiredLabel(conversationDetailsData)
                        }
                    } else {
                        MessageStatusLabel(messageStatus = message.header.messageStatus)
                    }
                    if (!isDeleted) {
                        if (!decryptionFailed) {
                            val currentOnAssetClicked = remember {
                                Clickable(enabled = isAvailable, onClick = {
                                    onAssetMessageClicked(header.messageId)
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }

                            val currentOnImageClick = remember {
                                Clickable(enabled = isAvailable, onClick = {
                                    onImageMessageClicked(
                                        message,
                                        source == MessageSource.Self
                                    )
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }
                            val onLongClick: (() -> Unit)? = remember {
                                if (isAvailable) {
                                    { onLongClicked(message) }
                                } else null
                            }
                            MessageContent(
                                message = message,
                                messageContent = messageContent,
                                audioMessagesState = audioMessagesState,
                                onAudioClick = onAudioClick,
                                onChangeAudioPosition = onChangeAudioPosition,
                                onAssetClick = currentOnAssetClicked,
                                onImageClick = currentOnImageClick,
                                onLongClick = onLongClick,
                                onOpenProfile = onOpenProfile
                            )
                            MessageFooter(
                                messageFooter,
                                onReactionClicked
                            )
                        } else {
                            MessageDecryptionFailure(
                                messageHeader = header,
                                decryptionStatus = header.messageStatus as MessageStatus.DecryptionFailure,
                                onResetSessionClicked = onResetSessionClicked
                            )
                        }
                        if (message.sendingFailed) {
                            MessageSendFailureWarning(
                                messageStatus = header.messageStatus as MessageStatus.MessageSendFailureStatus,
                                onRetryClick = remember { { onFailedMessageRetryClicked(header.messageId) } },
                                onCancelClick = remember { { onFailedMessageCancelClicked(header.messageId) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EphemeralMessageExpiredLabel(conversationDetailsData: ConversationDetailsData) {
    val stringResource = if (conversationDetailsData is ConversationDetailsData.OneOne) {
        conversationDetailsData.otherUserName?.let {
            stringResource(
                R.string.label_information_waiting_for_recipient_timer_to_expire_one_to_one,
                conversationDetailsData.otherUserName
            )
        } ?: stringResource(id = R.string.unknown_user_name)
    } else {
        stringResource(R.string.label_information_waiting_for_recipient_timer_to_expire_group)
    }

    Text(
        text = stringResource,
        style = typography().body05
    )
}

@Composable
fun MessageExpireLabel(messageContent: UIMessageContent?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.TextMessage -> {
            StatusBox(statusText = stringResource(R.string.self_deleting_message_time_left, timeLeft))
        }

        is UIMessageContent.AssetMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus.isSaved()) {
                    stringResource(
                        R.string.self_deleting_message_time_left,
                        timeLeft
                    )
                } else {
                    stringResource(R.string.self_deleting_message_label, timeLeft)
                }
            )
        }

        is UIMessageContent.AudioAssetMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus.isSaved()) {
                    stringResource(
                        R.string.self_deleting_message_time_left,
                        timeLeft
                    )
                } else {
                    stringResource(R.string.self_deleting_message_label, timeLeft)
                }
            )
        }

        is UIMessageContent.ImageMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus.isSaved()) {
                    stringResource(
                        R.string.self_deleting_message_time_left,
                        timeLeft
                    )
                } else {
                    stringResource(R.string.self_deleting_message_label, timeLeft)
                }
            )
        }

        is UIMessageContent.Deleted -> {
            val context = LocalContext.current

            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    context.resources.getQuantityString(
                        R.plurals.seconds_left,
                        0,
                        0
                    )
                )
            )
        }

        else -> {}
    }
}

@Composable
private fun MessageAuthorRow(messageHeader: MessageHeader) {
    with(messageHeader) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Username(username.asString(), modifier = Modifier.weight(weight = 1f, fill = false))
                UserBadge(
                    membership = membership,
                    connectionState = connectionState,
                    startPadding = dimensions().spacing6x,
                    isDeleted = isSenderDeleted
                )
                if (isLegalHold) {
                    LegalHoldIndicator(modifier = Modifier.padding(start = dimensions().spacing6x))
                }
            }
            MessageTimeLabel(
                time = messageHeader.messageTime.formattedDate,
                modifier = Modifier.padding(start = dimensions().spacing6x)
            )
        }
    }
}

@Composable
private fun MessageFooter(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit
) {
    if (messageFooter.reactions.entries.isNotEmpty()) { // to eliminate adding unnecessary paddings when the list is empty
        FlowRow(
            mainAxisSpacing = dimensions().spacing4x,
            crossAxisSpacing = dimensions().spacing6x,
            modifier = Modifier.padding(top = dimensions().spacing4x)
        ) {
            messageFooter.reactions.entries
                .sortedBy { it.key }
                .forEach {
                    val reaction = it.key
                    val count = it.value
                    ReactionPill(
                        emoji = reaction,
                        count = count,
                        isOwn = messageFooter.ownReactions.contains(reaction),
                        onTap = {
                            onReactionClicked(messageFooter.messageId, reaction)
                        }
                    )
                }
        }
    }
}

@Composable
private fun MessageTimeLabel(
    time: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun Username(username: String, modifier: Modifier = Modifier) {
    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Suppress("ComplexMethod")
@Composable
private fun MessageContent(
    message: UIMessage,
    messageContent: UIMessageContent.Regular?,
    audioMessagesState: Map<String, AudioState>,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> MessageImage(
            asset = messageContent.asset,
            imgParams = ImageMessageParams(messageContent.width, messageContent.height),
            uploadStatus = messageContent.uploadStatus,
            downloadStatus = messageContent.downloadStatus,
            onImageClick = onImageClick
        )

        is UIMessageContent.TextMessage -> {
            messageContent.messageBody.quotedMessage?.let {
                VerticalSpace.x4()
                when (it) {
                    is UIQuotedMessage.UIQuotedData -> QuotedMessage(it)
                    UIQuotedMessage.UnavailableData -> QuotedUnavailable(QuotedMessageStyle.COMPLETE)
                }
                VerticalSpace.x4()
            }
            MessageBody(
                messageBody = messageContent.messageBody,
                onLongClick = onLongClick,
                onOpenProfile = onOpenProfile
            )
        }

        is UIMessageContent.AssetMessage -> MessageGenericAsset(
            assetName = messageContent.assetName,
            assetExtension = messageContent.assetExtension,
            assetSizeInBytes = messageContent.assetSizeInBytes,
            assetUploadStatus = messageContent.uploadStatus,
            assetDownloadStatus = messageContent.downloadStatus,
            onAssetClick = onAssetClick
        )

        is UIMessageContent.RestrictedAsset -> {
            when {
                messageContent.mimeType.contains("image/") -> {
                    RestrictedAssetMessage(
                        R.drawable.ic_gallery,
                        stringResource(id = R.string.prohibited_images_message)
                    )
                }

                messageContent.mimeType.contains("video/") -> {
                    RestrictedAssetMessage(R.drawable.ic_video, stringResource(id = R.string.prohibited_videos_message))
                }

                messageContent.mimeType.contains("audio/") -> {
                    RestrictedAssetMessage(
                        R.drawable.ic_speaker_on,
                        stringResource(id = R.string.prohibited_audio_message)
                    )
                }

                else -> {
                    RestrictedGenericFileMessage(messageContent.assetName, messageContent.assetSizeInBytes)
                }
            }
        }

        is UIMessageContent.AudioAssetMessage -> {
            val audioMessageState: AudioState = audioMessagesState[message.header.messageId]
                ?: AudioState.DEFAULT

            val adjustedMessageState: AudioState = remember(audioMessagesState) {
                audioMessageState.sanitizeTotalTime(messageContent.audioMessageDurationInMs.toInt())
            }

            AudioMessage(
                audioMediaPlayingState = adjustedMessageState.audioMediaPlayingState,
                totalTimeInMs = adjustedMessageState.totalTimeInMs,
                currentPositionInMs = adjustedMessageState.currentPositionInMs,
                onPlayButtonClick = { onAudioClick(message.header.messageId) },
                onSliderPositionChange = { position ->
                    onChangeAudioPosition(message.header.messageId, position.toInt())
                },
                onAudioMessageLongClick = onLongClick
            )
        }

        UIMessageContent.Deleted -> {}
        null -> {
            throw NullPointerException("messageContent is null")
        }
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    messageStatus.badgeText?.let {
        StatusBox(it.asString())
    }
}

private fun Message.DownloadStatus.isSaved(): Boolean {
   return this == Message.DownloadStatus.SAVED_EXTERNALLY || this == Message.DownloadStatus.SAVED_INTERNALLY
}
