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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.flowlayout.FlowRow
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.messages.QuotedMessage
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
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserId

// TODO: a definite candidate for a refactor and cleanup
@Suppress("ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage,
    audioMessagesState: Map<String, AudioState>,
    onLongClicked: (UIMessage) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit
) {
    with(message) {
        val selfDeletionTimerState = rememberSelfDeletionTimer(expirationStatus)

        if (selfDeletionTimerState is SelfDeletionTimer.SelfDeletionTimerState.Expirable) {
            startDeletionTimer(
                message = message,
                expirableTimer = selfDeletionTimerState,
                onStartMessageSelfDeletion = onSelfDeletingMessageRead
            )
        }

        val backgroundColorModifier = if (message.sendingFailed || message.receivingFailed) {
            Modifier.background(colorsScheme().messageErrorBackgroundColor)
        } else if (selfDeletionTimerState is SelfDeletionTimer.SelfDeletionTimerState.Expirable) {
            val color by animateColorAsState(
                colorsScheme().primaryVariant.copy(selfDeletionTimerState.alphaBackgroundColor()),
                tween()
            )

            Modifier.background(color)
        } else {
            Modifier
        }

        Box(backgroundColorModifier) {
            val fullAvatarOuterPadding = dimensions().userAvatarClickablePadding + dimensions().userAvatarStatusBorderSize

            Row(
                Modifier
                    .padding(
                        end = dimensions().spacing16x,
                        bottom = dimensions().messageItemBottomPadding - fullAvatarOuterPadding
                    )
                    .fillMaxWidth()
                    .then(
                        if (!message.isDeleted) {
                            Modifier.combinedClickable(
                                // TODO: implement some action onClick
                                onClick = { },
                                onLongClick = { onLongClicked(message) }
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Spacer(Modifier.padding(start = dimensions().spacing8x - fullAvatarOuterPadding))

                val isProfileRedirectEnabled =
                    message.messageHeader.userId != null &&
                            !(message.messageHeader.isSenderDeleted || message.messageHeader.isSenderUnavailable)

                val avatarClickable = remember {
                    Clickable(enabled = isProfileRedirectEnabled) {
                        onOpenProfile(message.messageHeader.userId!!.toString())
                    }
                }
                UserProfileAvatar(
                    avatarData = message.userAvatarData,
                    clickable = avatarClickable
                )
                Spacer(Modifier.padding(start = dimensions().spacing16x - fullAvatarOuterPadding))
                Column {
                    Spacer(modifier = Modifier.height(fullAvatarOuterPadding))
                    MessageHeader(messageHeader)
                    if (selfDeletionTimerState is SelfDeletionTimer.SelfDeletionTimerState.Expirable) {
                        MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted())
                    }
                    if (!isDeleted) {
                        if (!decryptionFailed) {
                            val currentOnAssetClicked = remember {
                                Clickable(enabled = true, onClick = {
                                    onAssetMessageClicked(message.messageHeader.messageId)
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }

                            val currentOnImageClick = remember {
                                Clickable(enabled = true, onClick = {
                                    onImageMessageClicked(
                                        message,
                                        message.messageSource == MessageSource.Self
                                    )
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }

                            val onLongClick = remember { { onLongClicked(message) } }

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
                                messageHeader = messageHeader,
                                decryptionStatus = messageHeader.messageStatus as MessageStatus.DecryptionFailure,
                                onResetSessionClicked = onResetSessionClicked
                            )
                        }
                    }

                    if (message.sendingFailed) {
                        MessageSendFailureWarning(messageHeader.messageStatus as MessageStatus.MessageSendFailureStatus)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageExpireLabel(messageContent: UIMessageContent?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.TextMessage -> {
            StatusBox(statusText = stringResource(R.string.self_deleting_message_time_left, timeLeft))
        }

        is UIMessageContent.AssetMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY) stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
                else stringResource(R.string.self_deleting_message_label, timeLeft)
            )
        }

        is UIMessageContent.AudioAssetMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY) stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
                else stringResource(R.string.self_deleting_message_label, timeLeft)
            )
        }

        is UIMessageContent.ImageMessage -> {
            StatusBox(
                statusText = if (messageContent.downloadStatus == Message.DownloadStatus.SAVED_INTERNALLY) stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
                else stringResource(R.string.self_deleting_message_label, timeLeft)
            )
        }

        else -> {}
    }
}

@Composable
private fun MessageHeader(
    messageHeader: MessageHeader
) {
    with(messageHeader) {
        Column {
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
            MessageStatusLabel(messageStatus = messageStatus)
        }
    }
}

@Composable
private fun MessageFooter(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit
) {
    FlowRow(
        mainAxisSpacing = dimensions().spacing4x,
        crossAxisSpacing = dimensions().spacing6x,
        modifier = Modifier.padding(vertical = dimensions().spacing4x)
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
    messageContent: UIMessageContent?,
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
                QuotedMessage(it)
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
            val audioMessageState: AudioState = audioMessagesState[message.messageHeader.messageId]
                ?: AudioState.DEFAULT

            val adjustedMessageState: AudioState = remember(audioMessagesState) {
                audioMessageState.sanitizeTotalTime(messageContent.audioMessageDurationInMs.toInt())
            }

            AudioMessage(
                audioMediaPlayingState = adjustedMessageState.audioMediaPlayingState,
                totalTimeInMs = adjustedMessageState.totalTimeInMs,
                currentPositionInMs = adjustedMessageState.currentPositionInMs,
                onPlayButtonClick = { onAudioClick(message.messageHeader.messageId) },
                onSliderPositionChange = { position ->
                    onChangeAudioPosition(message.messageHeader.messageId, position.toInt())
                },
                onAudioMessageLongClick = onLongClick
            )
        }

        is UIMessageContent.SystemMessage.MemberAdded -> {}
        is UIMessageContent.SystemMessage.MemberJoined -> {}
        is UIMessageContent.SystemMessage.MemberLeft -> {}
        is UIMessageContent.SystemMessage.MemberRemoved -> {}
        is UIMessageContent.SystemMessage.RenamedConversation -> {}
        is UIMessageContent.SystemMessage.TeamMemberRemoved -> {}
        is UIMessageContent.SystemMessage.CryptoSessionReset -> {}
        is UIMessageContent.PreviewAssetMessage -> {}
        is UIMessageContent.SystemMessage.MissedCall.YouCalled -> {}
        is UIMessageContent.SystemMessage.MissedCall.OtherCalled -> {}
        is UIMessageContent.SystemMessage.NewConversationReceiptMode -> {}
        is UIMessageContent.SystemMessage.ConversationReceiptModeChanged -> {}
        null -> {
            throw NullPointerException("messageContent is null")
        }

        is UIMessageContent.SystemMessage.Knock -> {}
        is UIMessageContent.SystemMessage.HistoryLost -> {}
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    messageStatus.badgeText?.let {
        StatusBox(it.asString())
    }
}

@Composable
private fun MessageSendFailureWarning(
    messageStatus: MessageStatus.MessageSendFailureStatus
    /* TODO: add onRetryClick handler */
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_message_details_offline_backends_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            Text(
                text = messageStatus.errorText.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            if (messageStatus is MessageStatus.SendRemotelyFailure) {
                Text(
                    modifier = Modifier
                        .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                        textDecoration = TextDecoration.Underline
                    ),
                    text = stringResource(R.string.label_learn_more)
                )
            }
            // TODO: re-enable when we have a retry mechanism
//            VerticalSpace.x4()
//            Row {
//                WireSecondaryButton(
//                    text = stringResource(R.string.label_retry),
//                    onClick = { /* TODO */ },
//                    minHeight = dimensions().spacing32x,
//                    fillMaxWidth = false
//                )
//            }
        }
    }
}

@Composable
private fun MessageDecryptionFailure(
    messageHeader: MessageHeader,
    decryptionStatus: MessageStatus.DecryptionFailure,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_decryption_failure_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            VerticalSpace.x4()
            Text(
                text = decryptionStatus.errorText.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            Text(
                modifier = Modifier
                    .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                    textDecoration = TextDecoration.Underline
                ),
                text = stringResource(R.string.label_learn_more)
            )
            VerticalSpace.x4()
            Text(
                text = stringResource(R.string.label_message_decryption_failure_informative_message),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            if (!decryptionStatus.isDecryptionResolved) {
                Row {
                    WireSecondaryButton(
                        text = stringResource(R.string.label_reset_session),
                        onClick = {
                            messageHeader.userId?.let { userId ->
                                onResetSessionClicked(
                                    userId,
                                    messageHeader.clientId?.value
                                )
                            }
                        },
                        minHeight = dimensions().spacing32x,
                        fillMaxWidth = false
                    )
                }
            } else {
                VerticalSpace.x8()
            }
        }
    }
}
