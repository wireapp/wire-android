/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.compactLabel
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
fun MessageContentItem(
    clickActions: MessageClickActions,
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    assetStatus: AssetTransferStatus? = null,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    failureInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
    innerPadding: PaddingValues = PaddingValues(),
    isBubbleUiEnabled: Boolean = false,
) {
    val messageStyle = when {
        !isBubbleUiEnabled -> MessageStyle.NORMAL
        message.isMyMessage -> MessageStyle.BUBBLE_SELF
        else -> MessageStyle.BUBBLE_OTHER
    }
    with(message) {
        Column(
            modifier = modifier,
            horizontalAlignment = if (messageStyle == MessageStyle.BUBBLE_SELF) Alignment.End else Alignment.Start
        ) {
            if (showAuthor && !isBubbleUiEnabled) {
                MessageAuthorRow(
                    messageHeader = message.header, isBubbleUiEnabled = isBubbleUiEnabled, modifier = Modifier
                        .padding(innerPadding)
                        .padding(top = dimensions().avatarClickablePadding, bottom = dimensions().spacing4x)
                )
            }
            if (!isBubbleUiEnabled) {
                MessageStatusAndTimer(
                    message = message,
                    conversationDetailsData = conversationDetailsData,
                    selfDeletionTimerState = selfDeletionTimerState,
                )
            }

            if (isDeleted) {
                if (isBubbleUiEnabled) {
                    MessageBubbleEphemeralItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        selfDeletionTimerState = selfDeletionTimerState
                    )
                }

                return@Column
            }

            if (!decryptionFailed) {
                MessageContentAndStatus(
                    message = message,
                    assetStatus = assetStatus,
                    onAssetClicked = clickActions.onAssetClicked,
                    onImageClicked = clickActions.onImageClicked,
                    searchQuery = searchQuery,
                    onProfileClicked = clickActions.onProfileClicked,
                    onLinkClicked = clickActions.onLinkClicked,
                    shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                    conversationDetailsData = conversationDetailsData,
                    onReplyClicked = clickActions.onReplyClicked,
                    messageStyle = messageStyle
                )
                if (shouldDisplayFooter && !isBubbleUiEnabled) {
                    VerticalSpace.x4()
                    MessageReactionsItem(
                        messageFooter = messageFooter,
                        onReactionClicked = clickActions.onReactionClicked
                    )
                }
            } else {
                MessageDecryptionFailure(
                    messageHeader = header,
                    decryptionStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Decryption,
                    onResetSessionClicked = clickActions.onResetSessionClicked,
                    conversationProtocol = conversationDetailsData.conversationProtocol
                )
            }
            if (message.sendingFailed) {
                MessageSendFailureWarning(
                    messageStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                    isInteractionAvailable = failureInteractionAvailable,
                    onRetryClick = remember(message) {
                        {
                            clickActions.onFailedMessageRetryClicked(
                                header.messageId,
                                message.conversationId
                            )
                        }
                    },
                    onCancelClick = remember(message) {
                        {
                            clickActions.onFailedMessageCancelClicked(header.messageId)
                        }
                    }
                )
            }
            if (isBubbleUiEnabled && !useSmallBottomPadding) {

                // TODO KBX
                val bottomModifier = if (message.messageContent is UIMessageContent.ImageMessage) {
                    val imageMessage = (message.messageContent as UIMessageContent.ImageMessage)
                    val params = ImageMessageParams(imageMessage.width, imageMessage.height)
                    Modifier
//                        .width(max(params.normalizedSize().width, 180.dp))
                } else {
                    Modifier
                }

                VerticalSpace.x4()
                Row(
                    bottomModifier.padding(innerPadding),
                    horizontalArrangement = if (source == MessageSource.Self) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageTimeLabel(
                        messageTime = header.messageTime.formattedDate,
                        color = if (source == MessageSource.Self) {
                            colorsScheme().onPrimary
                        } else {
                            colorsScheme().secondaryText
                        }
                    )
                    MessageBubbleFooter(
                        message = message,
                        selfDeletionTimerState = selfDeletionTimerState,
                    )

                    if (isMyMessage && shouldDisplayMessageStatus) {
                        HorizontalSpace.x8()
                        MessageStatusIndicator(
                            status = message.header.messageStatus.flowStatus,
                            isGroupConversation = conversationDetailsData is ConversationDetailsData.Group,
                            messageStyle = messageStyle,
                            modifier = Modifier
                                .size(dimensions().spacing14x)
                        )
                    } else {
                        HorizontalSpace.x12()
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleEphemeralItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
) {
    with(message) {
        when (selfDeletionTimerState) {
            is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> {
                EphemeralMessageExpiredLabel(
                    message.isMyMessage,
                    conversationDetailsData,
                    color = if (source == MessageSource.Self) MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                        header.accent,
                        MaterialTheme.wireColorScheme.primary
                    ) else MaterialTheme.wireColorScheme.primary
                )
            }

            SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable -> {
                Text(
                    UIText.StringResource(R.string.deleted_message_text).asString(),
                    color = if (source == MessageSource.Self) {
                        MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                            header.accent,
                            MaterialTheme.wireColorScheme.primary
                        )
                    } else {
                        colorsScheme().secondaryText
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageBubbleFooter(
    message: UIMessage.Regular,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState,
    modifier: Modifier = Modifier,
) {
    with(message) {
        when (selfDeletionTimerState) {
            is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> {
                Row(modifier) {
                    HorizontalSpace.x8()
                    SelfDeletingMessageIcon(selfDeletionTimerState)
                    HorizontalSpace.x4()
                    MessageTimeLabel(
                        messageTime = selfDeletionTimerState.timeLeft.compactLabel(),
                        color = if (source == MessageSource.Self) {
                            colorsScheme().onPrimary
                        } else {
                            colorsScheme().secondaryText
                        }
                    )
                }
            }

            SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable -> {}
        }
    }
}

@Composable
private fun MessageStatusAndTimer(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState,
    modifier: Modifier = Modifier,
) {
    with(message) {
        if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted)
            // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
            // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
            // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
            if (isDeleted) {
                EphemeralMessageExpiredLabel(
                    message.isMyMessage,
                    conversationDetailsData
                )
            }
        } else {
            MessageStatusLabel(messageStatus = message.header.messageStatus)
        }
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    messageStatus.badgeText?.let {
        StatusBox(it.asString())
    }
}

@Composable
fun SelfDeletingMessageIcon(
    state: SelfDeletionTimerHelper.SelfDeletionTimerState,
    modifier: Modifier = Modifier
) {
    SelfDeletionTimerIcon(
        state = state,
        modifier = modifier,
        size = dimensions().spacing12x,
        discreteSteps = 8
    )
}

@Composable
fun SelfDeletionTimerIcon(
    state: SelfDeletionTimerHelper.SelfDeletionTimerState,
    modifier: Modifier = Modifier,
    size: Dp = 11.dp,
    filledColor: Color = MaterialTheme.colorScheme.secondary,
    emptyColor: Color = MaterialTheme.colorScheme.surface,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    discreteSteps: Int? = 8,
) {
    val fractionLeft = when (state) {
        is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> state.fractionLeft()
        is SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable -> 1f
    }

    val f = if (discreteSteps != null && discreteSteps > 0) {
        (kotlin.math.ceil(fractionLeft * discreteSteps) / discreteSteps)
            .coerceIn(0f, 1f)
    } else fractionLeft

    val elapsed = 1f - f
    val sweepEmpty = 360f * elapsed
    val startAtTop = -90f

    val bgAlpha = when (state) {
        is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> state.alphaBackgroundColor()
        else -> 0f
    }

    Canvas(
        modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "Time left ${"%.0f".format(f * 100)}%"
            }
    ) {
        if (bgAlpha > 0f) {
            drawCircle(color = filledColor.copy(alpha = bgAlpha))
        }

        drawCircle(color = filledColor)

        if (sweepEmpty > 0f) {
            drawArc(
                color = emptyColor,
                startAngle = startAtTop,
                sweepAngle = sweepEmpty,
                useCenter = true
            )
        }

        drawCircle(
            color = outlineColor,
            style = Stroke(width = size.value * 0.08f)
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactionsItem(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // to eliminate adding unnecessary paddings when the list is empty
    if (messageFooter.reactions.entries.isNotEmpty()) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x, Alignment.Top),
            maxItemsInEachRow = 4
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
                        },
                    )
                }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun LongMessageFooterPreview() = WireTheme {
    Box(modifier = Modifier.width(200.dp)) {
        MessageReactionsItem(
            messageFooter = MessageFooter(
                messageId = "messageId",
                reactions = mapOf(
                    "👍" to 1,
                    "👎" to 2,
                    "👏" to 3,
                    "🤔" to 4,
                    "🤷" to 5,
                    "🤦" to 6,
                    "🤢" to 7
                ),
                ownReactions = setOf("👍"),
            ),
            onReactionClicked = { _, _ -> }
        )
    }
}

