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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun MessageAuthorRow(
    messageHeader: MessageHeader,
    modifier: Modifier = Modifier
) {
    with(messageHeader) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
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
                if (showLegalHoldIndicator) {
                    LegalHoldIndicator(modifier = Modifier.padding(start = dimensions().spacing6x))
                }
            }
        }
    }
}

@Composable
private fun Username(username: String, accent: Accent, modifier: Modifier = Modifier) {
    val fallbackColor = if (accent == Accent.Unknown) {
        MaterialTheme.wireColorScheme.wireAccentColors.get(Accent.Unknown)
    } else {
        MaterialTheme.wireColorScheme.primary
    }

    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(accent, fallbackColor),
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun MessageSmallLabel(
    text: String,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = typography().subline01.copy(color = messageStyle.textColor()),
        maxLines = 1,
        modifier = modifier.alpha(messageStyle.alpha())
    )
}
