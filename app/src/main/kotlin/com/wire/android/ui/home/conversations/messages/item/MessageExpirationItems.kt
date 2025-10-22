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

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wire.android.R
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.UIText

@SuppressLint("ComposeModifierMissing")
@Composable
fun MessageBubbleEphemeralItem(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
) {
    with(message) {
        when (selfDeletionTimerState) {
            is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> {
                EphemeralMessageExpiredLabel(
                    isSelfMessage = message.isMyMessage,
                    conversationDetailsData = conversationDetailsData,
                    color = if (source == MessageSource.Self) {
                        MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                            header.accent,
                            MaterialTheme.wireColorScheme.primary
                        )
                    } else {
                        MaterialTheme.wireColorScheme.primary
                    }
                )
            }

            SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable -> {
                Text(
                    text = UIText.StringResource(R.string.deleted_message_text).asString(),
                    style = typography().body05,
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
fun MessageBubbleExpireFooter(
    messageStyle: MessageStyle,
    accentColor: Color,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState,
    modifier: Modifier = Modifier,
) {
    when (selfDeletionTimerState) {
        is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable -> {
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalSpace.x8()
                SelfDeletionTimerIcon(
                    selfDeletionTimerState,
                    messageStyle,
                    accentColor,
                    modifier = Modifier.alpha(messageStyle.alpha())
                )
                HorizontalSpace.x4()
                MessageSmallLabel(
                    text = selfDeletionTimerState.timeFormatted,
                    messageStyle = messageStyle
                )
            }
        }

        SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable -> {}
    }
}

@Composable
private fun SelfDeletionTimerIcon(
    state: SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable,
    messageStyle: MessageStyle,
    accentColor: Color,
    modifier: Modifier = Modifier,
    discreteSteps: Int? = 8
) {

    val emptyColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> accentColor
        MessageStyle.BUBBLE_OTHER -> colorsScheme().background
        MessageStyle.NORMAL -> colorsScheme().background
    }

    val filledColor = messageStyle.textColor()

    val metrics = state.iconMetrics(discreteSteps = discreteSteps)

    Canvas(
        modifier
            .size(dimensions().spacing12x)
            .semantics(mergeDescendants = true) {
                contentDescription = "Time left ${"%.0f".format(metrics.displayFractionLeft * 100)}%"
            }
    ) {
        val strokePx = this.size.minDimension * STROKE_WIDTH_FRACTION
        val insetPx = strokePx / 2f

        inset(insetPx, insetPx) {
            if (metrics.backgroundAlpha > 0f) {
                drawCircle(color = filledColor.copy(alpha = metrics.backgroundAlpha))
            }

            drawCircle(color = filledColor)

            if (metrics.emptySweepDegrees > 0f) {
                drawArc(
                    color = emptyColor,
                    startAngle = START_ANGLE_TOP_DEG,
                    sweepAngle = metrics.emptySweepDegrees,
                    useCenter = true
                )
            }

            drawCircle(
                color = filledColor,
                style = Stroke(width = this.size.minDimension * STROKE_WIDTH_FRACTION)
            )
        }
    }
}

@Suppress("ModifierParameter")
@Composable
fun MessageStatusAndExpireTimer(
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState,
) {
    with(message) {
        if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted)
            // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
            // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
            // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
            if (isDeleted) {
                EphemeralMessageExpiredLabel(
                    isSelfMessage = isMyMessage,
                    conversationDetailsData = conversationDetailsData
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
private fun EphemeralMessageExpiredLabel(
    isSelfMessage: Boolean,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {

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
        modifier = modifier,
        color = color,
        text = stringResource,
        style = typography().body05
    )
}

@Composable
fun MessageExpireLabel(messageContent: UIMessageContent?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.Location,
        is UIMessageContent.AssetMessage,
        is UIMessageContent.AudioAssetMessage,
        is UIMessageContent.ImageMessage,
        is UIMessageContent.TextMessage -> {
            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
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
