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
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.MessageAsset
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedAssetMessage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.RestrictedGenericFileMessage
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.home.conversations.model.messagetypes.location.LocationMessageContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.launchGeoIntent
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.asset.isSaved
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.PersistentMap
import kotlin.math.absoluteValue
import kotlin.math.min

// TODO: a definite candidate for a refactor and cleanup
@Suppress("ComplexMethod")
@Composable
fun RegularMessageItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    audioMessagesState: PersistentMap<String, AudioState>,
    onLongClicked: (UIMessage.Regular) -> Unit,
    onAssetMessageClicked: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    assetStatus: AssetTransferStatus? = null,
    swipableMessageConfiguration: SwipableMessageConfiguration = SwipableMessageConfiguration.NotSwipable,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
    onFailedMessageCancelClicked: (String) -> Unit = {},
    onLinkClick: (String) -> Unit = {},
    isContentClickable: Boolean = false,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    onReplyClickable: Clickable? = null,
    isInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable
): Unit = with(message) {
    @Composable
    fun messageContent() {
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
                        MessageAuthorRow(messageHeader = message.header)
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

                    if (isDeleted) return@Column

                    if (!decryptionFailed) {
                        MessageContentAndStatus(
                            message = message,
                            assetStatus = assetStatus,
                            onAssetMessageClicked = onAssetMessageClicked,
                            onLongClicked = onLongClicked,
                            isContentClickable = isContentClickable,
                            onImageMessageClicked = onImageMessageClicked,
                            searchQuery = searchQuery,
                            audioMessagesState = audioMessagesState,
                            onAudioClick = onAudioClick,
                            onChangeAudioPosition = onChangeAudioPosition,
                            onOpenProfile = onOpenProfile,
                            onLinkClick = onLinkClick,
                            shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                            conversationDetailsData = conversationDetailsData,
                            onReplyClickable = onReplyClickable

                        )
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
                            onResetSessionClicked = onResetSessionClicked,
                            conversationProtocol = conversationDetailsData.conversationProtocol
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
        )
    }
    if (swipableMessageConfiguration is SwipableMessageConfiguration.SwipableToReply && isReplyable) {
        val onSwipe = remember(message) { { swipableMessageConfiguration.onSwipedToReply(message) } }
        SwipableToReplyBox(onSwipedToReply = onSwipe) {
            messageContent()
        }
    } else {
        messageContent()
    }
}

@Stable
sealed interface SwipableMessageConfiguration {
    data object NotSwipable : SwipableMessageConfiguration
    class SwipableToReply(val onSwipedToReply: (uiMessage: UIMessage.Regular) -> Unit) : SwipableMessageConfiguration
}

enum class SwipeAnchor {
    CENTERED,
    START_TO_END
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipableToReplyBox(
    modifier: Modifier = Modifier,
    onSwipedToReply: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    var didVibrateOnCurrentDrag by remember { mutableStateOf(false) }

    // Finish the animation in the first 25% of the drag
    val progressUntilAnimationCompletion = 0.25f
    val dragWidth = screenWidth * progressUntilAnimationCompletion

    val currentViewConfiguration = LocalViewConfiguration.current
    val scopedViewConfiguration = object : ViewConfiguration by currentViewConfiguration {
        // Make it easier to scroll by giving the user a bit more length to identify the gesture as vertical
        override val touchSlop: Float
            get() = currentViewConfiguration.touchSlop * 3f
    }
    CompositionLocalProvider(LocalViewConfiguration provides scopedViewConfiguration) {
        val dragState = remember {
            AnchoredDraggableState(
                initialValue = SwipeAnchor.CENTERED,
                positionalThreshold = { dragWidth },
                velocityThreshold = { screenWidth },
                snapAnimationSpec = tween(),
                decayAnimationSpec = splineBasedDecay(density),
                confirmValueChange = { changedValue ->
                    if (changedValue == SwipeAnchor.START_TO_END) {
                        // Attempt to finish dismiss, notify reply intention
                        onSwipedToReply()
                    }
                    if (changedValue == SwipeAnchor.CENTERED) {
                        // Reset the haptic feedback when drag is stopped
                        didVibrateOnCurrentDrag = false
                    }
                    // Reject state change, only allow returning back to rest position
                    changedValue == SwipeAnchor.CENTERED
                },
                anchors = DraggableAnchors {
                    SwipeAnchor.CENTERED at 0f
                    SwipeAnchor.START_TO_END at screenWidth
                }
            )
        }
        val primaryColor = colorsScheme().primary

        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // Drag indication
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // TODO(RTL): Might need adjusting once RTL is supported
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(dragState.requireOffset().absoluteValue, size.height),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (dragState.offset > 0f) {
                    val dragProgress = dragState.offset / dragWidth
                    val adjustedProgress = min(1f, dragProgress)
                    val progress = FastOutLinearInEasing.transform(adjustedProgress)
                    // Got to the end, user can release to perform action, so we vibrate to show it
                    if (progress == 1f && !didVibrateOnCurrentDrag) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        didVibrateOnCurrentDrag = true
                    }

                    ReplySwipeIcon(dragWidth, density, progress)
                }
            }
            // Message content, which is draggable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .anchoredDraggable(dragState, Orientation.Horizontal, startDragImmediately = false)
                    .offset {
                        val x = dragState
                            .requireOffset()
                            .toInt()
                        IntOffset(x, 0)
                    },
            ) { content() }
        }
    }
}

@Composable
private fun ReplySwipeIcon(dragWidth: Float, density: Density, progress: Float) {
    val midPointBetweenStartAndGestureEnd = dragWidth / 2
    val iconSize = dimensions().fabIconSize
    val targetIconAnchorPosition = midPointBetweenStartAndGestureEnd - with(density) { iconSize.toPx() / 2 }
    val xOffset = with(density) {
        val totalTravelDistance = iconSize.toPx() + targetIconAnchorPosition
        -iconSize.toPx() + (totalTravelDistance * progress)
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

@Composable
private fun UIMessage.Regular.MessageContentAndStatus(
    message: UIMessage.Regular,
    assetStatus: AssetTransferStatus?,
    onAssetMessageClicked: (String) -> Unit,
    onLongClicked: (UIMessage.Regular) -> Unit,
    isContentClickable: Boolean,
    onImageMessageClicked: (UIMessage.Regular, Boolean) -> Unit,
    searchQuery: String,
    audioMessagesState: PersistentMap<String, AudioState>,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    shouldDisplayMessageStatus: Boolean,
    conversationDetailsData: ConversationDetailsData,
    onReplyClickable: Clickable?
) {
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
private fun MessageAuthorRow(messageHeader: MessageHeader) {
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
                messageTime = messageHeader.messageTime.formattedDate,
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
    messageTime: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = messageTime,
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
    onOpenProfile: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    clickable: Boolean,
    onLongClick: (() -> Unit)? = null,
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
                MessageAsset(
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
