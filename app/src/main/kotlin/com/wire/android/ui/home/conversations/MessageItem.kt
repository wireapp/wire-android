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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.ExpirationData
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
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// TODO: a definite candidate for a refactor and cleanup
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage,
    audioMessagesState: Map<String, AudioState>,
    onLongClicked: (UIMessage) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit
) {
    with(message) {
        val selfDeletionTimer = rememberSelfDeletionTimer(expirationData)

        LaunchedEffect(Unit) {
            onSelfDeletingMessageRead(message)

            while (selfDeletionTimer.timeLeft.inWholeSeconds >= 0) {
                delay(selfDeletionTimer.interval())
                selfDeletionTimer.decreaseTimeLeft(selfDeletionTimer.interval())
            }
        }

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
                .then(
                    if (message.sendingFailed || message.receivingFailed) {
                        Modifier.background(MaterialTheme.wireColorScheme.messageErrorBackgroundColor)
                    } else if (selfDeletionTimer.timeLeft != Duration.ZERO) {
                        val color by animateColorAsState(
                            MaterialTheme.wireColorScheme.primary.copy(selfDeletionTimer.alphaBackgroundColor()),
                            tween()
                        )

                        Modifier.background(color)
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
                MessageExpireLabel(selfDeletionTimer)
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
                                    message.messageHeader.messageId,
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
                    MessageSendFailureWarning(messageHeader.messageStatus)
                }
            }
        }
    }
}

class SelfDeletionTimer(expirationData: ExpirationData?) {
    companion object {
        private const val DAYS_IN_A_WEEK = 7
        private const val FOUR_WEEK_DAYS = DAYS_IN_A_WEEK * 4
        private const val THREE_WEEK_DAYS = DAYS_IN_A_WEEK * 3
        private const val TWO_WEEK_DAYS = DAYS_IN_A_WEEK * 2
        private const val ONE_WEEK_DAYS = DAYS_IN_A_WEEK * 1
    }

    var timeLeft by mutableStateOf(expirationData?.timeLeft ?: 0.seconds)

    private val expireAfter = expirationData?.expireAfter ?: 0.seconds
    fun timeLeftFormatted(): String {
        val timeLeftLabel = when {
            // weeks
            timeLeft.inWholeDays >= FOUR_WEEK_DAYS -> "4 weeks"
            timeLeft.inWholeDays in THREE_WEEK_DAYS until FOUR_WEEK_DAYS -> "3 weeks"
            timeLeft.inWholeDays in TWO_WEEK_DAYS until THREE_WEEK_DAYS -> "2 weeks"
            timeLeft.inWholeDays in ONE_WEEK_DAYS until TWO_WEEK_DAYS -> "1 week"
            // days
            timeLeft.inWholeDays in 1..6 -> "${timeLeft.inWholeDays} days left"
            // hours
            timeLeft.inWholeHours in 1..23 -> "${timeLeft.inWholeHours} hours left"
            // minutes
            timeLeft.inWholeMinutes in 1..59 -> "${timeLeft.inWholeMinutes} minutes left"
            // seconds
            timeLeft.inWholeSeconds < 60 -> "${timeLeft.inWholeSeconds} seconds left "

            else -> throw IllegalStateException("Not possible state for a time left label")
        }

        return timeLeftLabel
    }

    fun interval(): Duration {
        val interval = when {
            timeLeft.inWholeMinutes > 59 -> 1.hours
            timeLeft.inWholeMinutes in 2..59 -> 1.minutes
            timeLeft.inWholeSeconds <= 60 && timeLeft.inWholeMinutes < 2 -> 1.seconds
            else -> throw IllegalStateException("Not possible state for interval")
        }

        return interval
    }

    fun decreaseTimeLeft(interval: Duration) {
        if (timeLeft.inWholeSeconds != 0L) timeLeft -= interval
    }

    fun alphaBackgroundColor(): Float {
        val totalTimeLeftRatio = timeLeft / expireAfter

        return if (totalTimeLeftRatio >= 0.75) {
            0F
        } else {
            1F
        }
    }

}

@Composable
fun rememberSelfDeletionTimer(expirationData: ExpirationData?): SelfDeletionTimer {
    return SelfDeletionTimer(expirationData)
}

@Composable
fun MessageExpireLabel(selfDeletionTimer: SelfDeletionTimer) {
    if (selfDeletionTimer.timeLeft != Duration.ZERO) {
        Text("Self-deleting message • ${selfDeletionTimer.timeLeftFormatted()}")
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
    when (messageStatus) {
        MessageStatus.Deleted,
        is MessageStatus.Edited,
        MessageStatus.ReceiveFailure -> StatusBox(messageStatus.text.asString())

        is MessageStatus.DecryptionFailure,
        is MessageStatus.SendRemotelyFailure, MessageStatus.SendFailure, MessageStatus.Untouched -> {
            /** Don't display anything **/
        }
    }
}

@Composable
private fun MessageSendFailureWarning(
    messageStatus: MessageStatus
    /* TODO: add onRetryClick handler */
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_message_details_offline_backends_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            Text(
                text = messageStatus.text.asString(),
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
                text = decryptionStatus.text.asString(),
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
