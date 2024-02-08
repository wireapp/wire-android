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

package com.wire.android.ui.home.conversations

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.QuotedMessage
import com.wire.android.ui.home.conversations.messages.QuotedMessageStyle
import com.wire.android.ui.home.conversations.messages.QuotedUnavailable
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
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
import com.wire.android.ui.home.conversations.model.messagetypes.location.LocationMessageContent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.launchGeoIntent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.PersistentMap

// TODO: a definite candidate for a refactor and cleanup
@Suppress("ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    audioMessagesState: PersistentMap<String, AudioState>,
    onLongClicked: (UIMessage.Regular) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    onFailedMessageRetryClicked: (String) -> Unit = {},
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    isContentClickable: Boolean = false,
    onMessageClick: (messageId: String) -> Unit = {},
    defaultBackgroundColor: Color = Color.Transparent,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    onReplyClickable: Clickable? = null,
    isSelectedMessage: Boolean = false
) {
    with(message) {
        val selfDeletionTimerState = rememberSelfDeletionTimer(header.messageStatus.expirationStatus)
        if (
            selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable &&
            !message.isPending &&
            !message.sendingFailed
        ) {
            selfDeletionTimerState.startDeletionTimer(
                message = message,
                onStartMessageSelfDeletion = onSelfDeletingMessageRead
            )
        }

        var backgroundColorModifier = if (message.sendingFailed || message.decryptionFailed) {
            Modifier.background(colorsScheme().messageErrorBackgroundColor)
        } else if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable && !message.isDeleted) {
            val color by animateColorAsState(
                colorsScheme().primaryVariant.copy(selfDeletionTimerState.alphaBackgroundColor()),
                tween(),
                label = "message background color"
            )

            Modifier.background(color)
        } else {
            Modifier.background(defaultBackgroundColor)
        }

        val colorAnimation = remember { Animatable(Color.Transparent) }
        val highlightColor = colorsScheme().selectedMessageHighlightColor
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

        if (isSelectedMessage) {
            backgroundColorModifier = Modifier.drawBehind { drawRect(colorAnimation.value) }
        }

        Box(
            backgroundColorModifier
                .combinedClickable(enabled = true, onClick = {
                    if (isContentClickable) {
                        onMessageClick(message.header.messageId)
                    }
                },
                    onLongClick = remember(message) {
                        {
                            if (!isContentClickable) {
                                onLongClicked(message)
                            }
                        }
                    }
                )
        ) {
            // padding needed to have same top padding for avatar and rest composables in message item
            val fullAvatarOuterPadding = dimensions().avatarClickablePadding + dimensions().avatarStatusBorderSize
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        end = dimensions().messageItemHorizontalPadding,
                        top = if (showAuthor) dimensions().spacing0x else dimensions().spacing4x,
                        bottom = if (useSmallBottomPadding) dimensions().spacing2x else dimensions().messageItemBottomPadding
                    )
            ) {
                val isProfileRedirectEnabled =
                    header.userId != null &&
                            !(header.isSenderDeleted || header.isSenderUnavailable)

                Box(
                    contentAlignment = Alignment.TopStart
                ) {
                    if (showAuthor) {
                        val avatarClickable = remember {
                            Clickable(enabled = isProfileRedirectEnabled) {
                                onOpenProfile(header.userId!!.toString())
                            }
                        }
                        // because avatar takes start padding we don't need to add padding to message item
                        UserProfileAvatar(
                            avatarData = message.userAvatarData,
                            clickable = if (isContentClickable) null else avatarClickable
                        )
                    } else {
                        // imitating width of space that avatar takes
                        Spacer(
                            Modifier.width(
                                dimensions().avatarDefaultSize
                                        + (dimensions().avatarStatusBorderSize * 2)
                                        + (dimensions().avatarClickablePadding * 2)
                            )
                        )
                    }
                }
                Spacer(Modifier.width(dimensions().messageItemHorizontalPadding - fullAvatarOuterPadding))
                Column {
                    if (showAuthor) {
                        Spacer(modifier = Modifier.height(dimensions().avatarClickablePadding))
                        MessageAuthorRow(messageHeader = message.header)
                    }
                    if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
                        MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted)

                        // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
                        // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
                        // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
                        if (isDeleted) {
                            EphemeralMessageExpiredLabel(message.isMyMessage, conversationDetailsData)
                        }
                    } else {
                        MessageStatusLabel(messageStatus = message.header.messageStatus)
                    }
                    if (!isDeleted) {
                        if (!decryptionFailed) {
                            val currentOnAssetClicked = remember(message) {
                                Clickable(enabled = isAvailable, onClick = {
                                    onAssetMessageClicked(header.messageId)
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }

                            val currentOnImageClick = remember(message) {
                                Clickable(enabled = isAvailable && !isContentClickable, onClick = {
                                    onImageMessageClicked(
                                        message,
                                        source == MessageSource.Self
                                    )
                                }, onLongClick = {
                                    onLongClicked(message)
                                })
                            }
                            val onLongClick: (() -> Unit)? = if (isContentClickable) null else remember(message) {
                                if (isAvailable) {
                                    { onLongClicked(message) }
                                } else {
                                    null
                                }
                            }
                            Row {
                                Box(modifier = Modifier.weight(1F)) {
                                    MessageContent(
                                        message = message,
                                        messageContent = messageContent,
                                        searchQuery = searchQuery,
                                        audioMessagesState = audioMessagesState,
                                        onAudioClick = onAudioClick,
                                        onChangeAudioPosition = onChangeAudioPosition,
                                        onAssetClick = currentOnAssetClicked,
                                        onImageClick = currentOnImageClick,
                                        onLongClick = onLongClick,
                                        onOpenProfile = onOpenProfile,
                                        onLinkClick = onLinkClick,
                                        clickable = !isContentClickable,
                                        onReplyClickable = onReplyClickable
                                    )
                                }
                                if (isMyMessage && shouldDisplayMessageStatus) {
                                    MessageStatusIndicator(
                                        status = message.header.messageStatus.flowStatus,
                                        isGroupConversation = conversationDetailsData is ConversationDetailsData.Group,
                                        modifier = Modifier.padding(
                                            top = if (message.isTextContentWithoutQuote) dimensions().spacing2x else dimensions().spacing4x,
                                            start = dimensions().spacing8x
                                        )
                                    )
                                } else {
                                    HorizontalSpace.x24()
                                }
                            }
                            if (shouldDisplayFooter) {
                                VerticalSpace.x4()
                                MessageFooter(
                                    messageFooter = messageFooter,
                                    onReactionClicked = onReactionClicked
                                )
                            }
                        } else {
                            MessageDecryptionFailure(
                                messageHeader = header,
                                decryptionStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Decryption,
                                onResetSessionClicked = onResetSessionClicked
                            )
                        }
                        if (message.sendingFailed) {
                            MessageSendFailureWarning(
                                messageStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                                onRetryClick = remember { { onFailedMessageRetryClicked(header.messageId) } },
                                onCancelClick = remember { { onFailedMessageCancelClicked(header.messageId) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EphemeralMessageExpiredLabel(isSelfMessage: Boolean, conversationDetailsData: ConversationDetailsData) {

    val stringResource = if (!isSelfMessage) {
        stringResource(id = R.string.label_information_waiting_for_deleation_when_self_not_sender)
    } else if (conversationDetailsData is ConversationDetailsData.OneOne) {
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
        is UIMessageContent.Location,
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
                Username(
                    username.asString(),
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageFooter(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit
) {
    // to eliminate adding unnecessary paddings when the list is empty
    if (messageFooter.reactions.entries.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x, Alignment.Top),
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
    message: UIMessage.Regular,
    messageContent: UIMessageContent.Regular?,
    searchQuery: String,
    audioMessagesState: Map<String, AudioState>,
    onAssetClick: Clickable,
    onImageClick: Clickable,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onLongClick: (() -> Unit)? = null,
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    clickable: Boolean,
    onReplyClickable: Clickable? = null
) {
    when (messageContent) {
        is UIMessageContent.ImageMessage -> {
            Column {
                MessageImage(
                    asset = messageContent.asset,
                    imgParams = ImageMessageParams(messageContent.width, messageContent.height),
                    uploadStatus = messageContent.uploadStatus,
                    downloadStatus = messageContent.downloadStatus,
                    onImageClick = onImageClick
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.TextMessage -> {
            Column {
                messageContent.messageBody.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            messageData = it,
                            clickable = onReplyClickable
                        )
                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(style = QuotedMessageStyle.COMPLETE)
                    }
                    VerticalSpace.x4()
                }
                MessageBody(
                    messageBody = messageContent.messageBody,
                    searchQuery = searchQuery,
                    isAvailable = !message.isPending && message.isAvailable,
                    onLongClick = onLongClick,
                    onOpenProfile = onOpenProfile,
                    buttonList = null,
                    messageId = message.header.messageId,
                    onLinkClick = onLinkClick,
                    clickable = clickable
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.Composite -> {
            Column {
                messageContent.messageBody?.quotedMessage?.let {
                    VerticalSpace.x4()
                    when (it) {
                        is UIQuotedMessage.UIQuotedData -> QuotedMessage(
                            messageData = it,
                            clickable = onReplyClickable
                        )
                        UIQuotedMessage.UnavailableData -> QuotedUnavailable(style = QuotedMessageStyle.COMPLETE)
                    }
                    VerticalSpace.x4()
                }
                MessageBody(
                    messageBody = messageContent.messageBody,
                    isAvailable = !message.isPending && message.isAvailable,
                    onLongClick = onLongClick,
                    onOpenProfile = onOpenProfile,
                    buttonList = messageContent.buttonList,
                    messageId = message.header.messageId,
                    onLinkClick = onLinkClick
                )
            }
        }

        is UIMessageContent.AssetMessage -> {
            Column {
                MessageGenericAsset(
                    assetName = messageContent.assetName,
                    assetExtension = messageContent.assetExtension,
                    assetSizeInBytes = messageContent.assetSizeInBytes,
                    assetUploadStatus = messageContent.uploadStatus,
                    assetDownloadStatus = messageContent.downloadStatus,
                    onAssetClick = onAssetClick
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.RestrictedAsset -> {
            Column {
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
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.AudioAssetMessage -> {
            Column {
                val audioMessageState: AudioState = audioMessagesState[message.header.messageId]
                    ?: AudioState.DEFAULT

                val totalTimeInMs = remember(audioMessageState.totalTimeInMs) {
                    audioMessageState.sanitizeTotalTime(messageContent.audioMessageDurationInMs.toInt())
                }

                AudioMessage(
                    audioMediaPlayingState = audioMessageState.audioMediaPlayingState,
                    totalTimeInMs = totalTimeInMs,
                    currentPositionInMs = audioMessageState.currentPositionInMs,
                    onPlayButtonClick = { onAudioClick(message.header.messageId) },
                    onSliderPositionChange = { position ->
                        onChangeAudioPosition(message.header.messageId, position.toInt())
                    },
                    onAudioMessageLongClick = onLongClick
                )
                PartialDeliveryInformation(messageContent.deliveryStatus)
            }
        }

        is UIMessageContent.Location -> with(messageContent) {
            val context = LocalContext.current
            val locationUrl = stringResource(urlCoordinates, zoom, latitude, longitude)
            Column {
                LocationMessageContent(
                    locationName = name,
                    locationUrl = locationUrl,
                    onLocationClick = Clickable(
                        enabled = message.isAvailable,
                        onClick = { launchGeoIntent(latitude, longitude, name, locationUrl, context) },
                        onLongClick = onLongClick
                    )
                )
                PartialDeliveryInformation(deliveryStatus)
            }
        }

        UIMessageContent.Deleted -> {}
        null -> {
            throw NullPointerException("messageContent is null")
        }
    }
}

@Composable
private fun PartialDeliveryInformation(deliveryStatus: DeliveryStatusContent) {
    (deliveryStatus as? DeliveryStatusContent.PartialDelivery)?.let { partialDelivery ->
        if (partialDelivery.hasFailures) {
            VerticalSpace.x4()
            MessageSentPartialDeliveryFailures(partialDelivery)
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

internal val DeliveryStatusContent.expandable
    get() = this is DeliveryStatusContent.PartialDelivery && !this.isSingleUserFailure

private const val SELECTED_MESSAGE_ANIMATION_DURATION = 2000
