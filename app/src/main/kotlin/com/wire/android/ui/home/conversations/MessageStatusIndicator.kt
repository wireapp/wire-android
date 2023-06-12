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
 */
package com.wire.android.ui.home.conversations

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MessageStatusIndicator(message: UIMessage.Regular) {
    when {
        message.isPending -> Icon(
            painter = painterResource(id = R.drawable.ic_message_sending),
            tint = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
            contentDescription = stringResource(R.string.content_description_message_sending_status),
        )

        !message.isAvailable -> Icon(
            painter = painterResource(id = R.drawable.ic_message_error),
            tint = MaterialTheme.wireColorScheme.error,
            contentDescription = stringResource(R.string.content_description_message_error_status),
        )
        // TODO handle read, sent and delivered status
//        !message.isPending -> Icon(
//            painter = painterResource(id = R.drawable.ic_message_delivered),
//            tint = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
//            contentDescription = stringResource(R.string.content_description_message_delivered_status),
//        )
        else -> {}
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusFailed() {
    WireTheme {
        MessageStatusIndicator(
            mockMessageWithText.copy(
                header = mockHeader.copy(
                    messageStatus = MessageStatus.SendFailure
                )
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusSending() {
    WireTheme {
        MessageStatusIndicator(
            mockMessageWithText.copy(
                header = mockHeader.copy(messageStatus = MessageStatus.Untouched(isPending = true))
            )
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageStatusDelivered() {
    WireTheme {
        MessageStatusIndicator(
            mockMessageWithText.copy(
                header = mockHeader.copy(messageStatus = MessageStatus.Untouched(isPending = false))
            )
        )
    }
}
