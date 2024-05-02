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

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
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
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.location.LocationMessageContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.MessageDateTime
import com.wire.android.util.launchGeoIntent
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isSaved
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.absoluteValue
import kotlin.math.min

// TODO: a definite candidate for a refactor and cleanup
@Suppress("ComplexMethod")
@Composable
fun RegularMessageItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    audioMessagesState: PersistentMap<String, AudioState>,
    assetStatus: AssetTransferStatus? = null,
    onLongClicked: (UIMessage.Regular) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit = {},
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    isContentClickable: Boolean = false,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    onReplyClickable: Clickable? = null,
    isInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    currentTimeInMillisFlow: Flow<Long> = flow { },
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable
): Unit = with(message) {
    val onSwipe = remember(message) { { onSwipedToReply(message) } }
    SwipableToReplyBox(
        isSwipable = isReplyable,
        onSwipedToReply = onSwipe
    ) {
        MessageItemTemplate(
            showAuthor,
            useSmallBottomPadding = useSmallBottomPadding,
            fullAvatarOuterPadding = dimensions().avatarClickablePadding + dimensions().avatarStatusBorderSize,
            leading = {
                RegularMessageItemLeading(
                    header = header,
                    showAuthor = showAuthor,
                    userAvatarData = message.userAvatarData,
                    isContentClickable = isContentClickable,
                    onOpenProfile = onOpenProfile
                )
            },
            content = {
                Column {
                    if (showAuthor) {
                        Spacer(modifier = Modifier.height(dimensions().avatarClickablePadding))
                        MessageAuthorRow(messageHeader = message.header, currentTimeInMillisFlow)
                        Spacer(modifier = Modifier.height(dimensions().spacing4x))
                    }
                    if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
                        MessageExpireLabel(messageContent, assetStatus, selfDeletionTimerState.timeLeftFormatted)

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
                            val onLongClick: (() -> Unit)? = if (isContentClickable) {
                                null
                            } else {
                                remember(message) {
                                    if (isAvailable) {
                                        { onLongClicked(message) }
                                    } else {
                                        null
                                    }
                                }
                            }
                            Row {
                                Box(modifier = Modifier.weight(1F)) {
                                    MessageContent(
                                        message = message,
                                        messageContent = messageContent,
                                        searchQuery = searchQuery,
                                        audioMessagesState = audioMessagesState,
                                        assetStatus = assetStatus,
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
                                isInteractionAvailable = isInteractionAvailable,
                                onRetryClick = remember { { onFailedMessageRetryClicked(header.messageId, message.conversationId) } },
                                onCancelClick = remember { { onFailedMessageCancelClicked(header.messageId) } }
                            )
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipableToReplyBox(
    isSwipable: Boolean,
    modifier: Modifier = Modifier,
    onSwipedToReply: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var didVibrateOnCurrentDrag by remember { mutableStateOf(false) }

    // Finish the animation in the first 25% of the drag
    val progressUntilAnimationCompletion = 0.25f
    val dismissState = remember {
        SwipeToDismissBoxState(
            SwipeToDismissBoxValue.Settled,
            density,
            positionalThreshold = { distance: Float -> distance * progressUntilAnimationCompletion },
            confirmValueChange = { changedValue ->
                if (changedValue == SwipeToDismissBoxValue.StartToEnd) {
                    onSwipedToReply()
                }
                if (changedValue == SwipeToDismissBoxValue.Settled) {
                    // Reset the haptic feedback when drag is stopped
                    didVibrateOnCurrentDrag = false
                }
                // Go back to rest position
                changedValue == SwipeToDismissBoxValue.Settled
            }
        )
    }
    val primaryColor = colorsScheme().primary
    // TODO: RTL is currently broken https://issuetracker.google.com/issues/321600474
    //       Maybe addressed in compose3 1.3.0 (currently in alpha)
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = isSwipable,
        content = content,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize()
                    .drawBehind {
                        // TODO(RTL): Might need adjusting once RTL is supported (also lacking in SwipeToDismissBox)
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(dismissState.requireOffset().absoluteValue, size.height),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                    // Sometimes this is called with progress 1f when the user stops the interaction, causing a blink.
                    // Ignore these cases as it doesn't make any difference
                    && dismissState.progress < 1f
                ) {
                    val adjustedProgress = min(1f, (dismissState.progress / progressUntilAnimationCompletion))
                    val iconSize = dimensions().fabIconSize
                    val spacing = dimensions().spacing16x
                    val progress = FastOutLinearInEasing.transform(adjustedProgress)
                    val xOffset = with(density) {
                        val offsetBeforeScreenStart = iconSize.toPx()
                        val offsetAfterScreenStart = spacing.toPx()
                        val totalTravelDistance = offsetBeforeScreenStart + offsetAfterScreenStart
                        -offsetBeforeScreenStart + (totalTravelDistance * progress)
                    }
                    // Got to the end, user can release to
                    if (progress == 1f && !didVibrateOnCurrentDrag) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        didVibrateOnCurrentDrag = true
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_reply),
                        contentDescription = "",
                        modifier = Modifier
                            .size(iconSize)
                            .offset { IntOffset(xOffset.toInt(), 0) },
                        tint = colorsScheme().onPrimary
                    )
                }
            }
        }
    )
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
fun MessageExpireLabel(messageContent: UIMessageContent?, assetTransferStatus: AssetTransferStatus?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.Location,
        is UIMessageContent.TextMessage -> {
            StatusBox(statusText = stringResource(R.string.self_deleting_message_time_left, timeLeft))
        }

        is UIMessageContent.AssetMessage -> {
            StatusBox(
                statusText = if (assetTransferStatus.isSaved()) {
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
                statusText = if (assetTransferStatus.isSaved()) {
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
                statusText = if (assetTransferStatus.isSaved()) {
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
private fun MessageAuthorRow(messageHeader: MessageHeader, currentTimeInMillisFlow: Flow<Long>) {
    with(messageHeader) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Username(
                    username.asString(),
                    accent,
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
                messageTime = messageHeader.messageTime,
                currentTimeInMillisFlow = currentTimeInMillisFlow,
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
    messageTime: MessageTime,
    currentTimeInMillisFlow: Flow<Long>,
    modifier: Modifier = Modifier
) {

    val currentTime by currentTimeInMillisFlow.collectAsState(initial = System.currentTimeMillis())

    val messageDateTime = messageTime.formattedDate(now = currentTime)

    val context = LocalContext.current

    val timeString = when (messageDateTime) {
        is MessageDateTime.Now -> context.resources.getString(R.string.message_datetime_now)
        is MessageDateTime.Within30Minutes -> context.resources.getQuantityString(
            R.plurals.message_datetime_minutes_ago,
            messageDateTime.minutes,
            messageDateTime.minutes
        )

        is MessageDateTime.Today -> context.resources.getString(R.string.message_datetime_today, messageDateTime.time)
        is MessageDateTime.Yesterday -> context.resources.getString(R.string.message_datetime_yesterday, messageDateTime.time)
        is MessageDateTime.WithinWeek -> context.resources.getString(R.string.message_datetime_other, messageDateTime.date)
        is MessageDateTime.NotWithinWeekButSameYear -> context.resources.getString(R.string.message_datetime_other, messageDateTime.date)
        is MessageDateTime.Other -> context.resources.getString(R.string.message_datetime_other, messageDateTime.date)
        null -> ""
    }

    Text(
        text = timeString,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun Username(username: String, accent: Accent, modifier: Modifier = Modifier) {
    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(accent, MaterialTheme.wireColorScheme.onBackground),
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
    audioMessagesState: PersistentMap<String, AudioState>,
    assetStatus: AssetTransferStatus?,
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
                    transferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
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
                    assetTransferStatus = assetStatus ?: AssetTransferStatus.NOT_DOWNLOADED,
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

internal val DeliveryStatusContent.expandable
    get() = this is DeliveryStatusContent.PartialDelivery && !this.isSingleUserFailure
