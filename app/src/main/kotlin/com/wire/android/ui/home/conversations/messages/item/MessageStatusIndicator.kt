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

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MessageStatusIndicator(
    status: MessageFlowStatus,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    isGroupConversation: Boolean = false,
) {
    val defaultTint = when(messageStyle) {
        MessageStyle.BUBBLE_SELF -> MaterialTheme.wireColorScheme.onPrimary
        MessageStyle.BUBBLE_OTHER -> MaterialTheme.wireColorScheme.secondaryText
        MessageStyle.NORMAL -> MaterialTheme.wireColorScheme.onTertiaryButtonDisabled
    }

    when (status) {
        MessageFlowStatus.Sending -> Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_message_sending),
            tint = defaultTint,
            contentDescription = stringResource(R.string.content_description_message_sending_status),
        )

        MessageFlowStatus.Sent -> {
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.ic_message_sent),
                tint = defaultTint,
                contentDescription = stringResource(R.string.content_description_message_sending_status),
            )
        }

        MessageFlowStatus.Delivered -> {
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.ic_message_delivered),
                tint = defaultTint,
                contentDescription = stringResource(R.string.content_description_message_delivered_status),
            )
        }

        is MessageFlowStatus.Read -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = modifier,
                    painter = painterResource(id = R.drawable.ic_message_read),
                    tint = defaultTint,
                    contentDescription = stringResource(R.string.content_description_message_read_status),
                )
                if (isGroupConversation) {
                    HorizontalSpace.x2()
                    Text(
                        text = status.count.toString(),
                        style = MaterialTheme.wireTypography.label03.copy(color = defaultTint)
                    )
                }
            }
        }

        is MessageFlowStatus.Failure -> Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_warning_circle),
            tint = MaterialTheme.wireColorScheme.error,
            contentDescription = stringResource(R.string.content_description_message_error_status),
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusFailed() {
    WireTheme {
        MessageStatusIndicator(MessageFlowStatus.Failure.Send.Locally(false))
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusSending() {
    WireTheme {
        MessageStatusIndicator(MessageFlowStatus.Sending)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusRead() {
    WireTheme {
        MessageStatusIndicator(MessageFlowStatus.Read(1))
    }
}
