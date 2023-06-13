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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MessageStatusIndicator(status: MessageFlowStatus, modifier: Modifier = Modifier) {
    when (status) {
        MessageFlowStatus.Sending -> Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_message_sending),
            tint = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
            contentDescription = stringResource(R.string.content_description_message_sending_status),
        )

        is MessageFlowStatus.Failure -> Icon(
            modifier = modifier,
            painter = painterResource(id = R.drawable.ic_message_error),
            tint = MaterialTheme.wireColorScheme.error,
            contentDescription = stringResource(R.string.content_description_message_error_status),
        )
        // TODO handle read, sent and delivered status
//        MessageFlowStatus.Sent -> Icon(
//            modifier = modifier,
//            painter = painterResource(id = R.drawable.ic_message_delivered),
//            tint = MaterialTheme.wireColorScheme.onTertiaryButtonDisabled,
//            contentDescription = stringResource(R.string.content_description_message_delivered_status),
//        )
//        is MessageFlowStatus.Read -> {}
//        MessageFlowStatus.Delivered -> TODO()
        else -> HorizontalSpace.x16()
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
