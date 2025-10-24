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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.R
import com.wire.android.ui.common.channelConversationColor
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun ChannelConversationAvatar(
    conversationId: ConversationId,
    isPrivateChannel: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize,
    cornerRadius: Dp = MaterialTheme.wireDimensions.groupAvatarCornerRadius,
    padding: Dp = MaterialTheme.wireDimensions.avatarClickablePadding,
    borderWidth: Dp = dimensions().avatarBorderWidth,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val colors = colorsScheme().channelConversationColor(conversationId)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(padding)
                .size(size)
                .border(color = colors.border, width = borderWidth, shape = RoundedCornerShape(cornerRadius + borderWidth))
                .padding(borderWidth)
                .background(color = colors.background, shape = RoundedCornerShape(cornerRadius))
        ) {
            Icon(
                modifier = Modifier.padding(size / 4),
                painter = painterResource(id = R.drawable.ic_channel),
                contentDescription = null,
                tint = colors.icon
            )
        }
        if (isPrivateChannel) {
            val scale = 0.4375f // 14/32
            val offset = (size - borderWidth * 2) * scale
            ChannelLock(
                size = size * scale,
                cornerRadius = cornerRadius * scale,
                modifier = Modifier
                    .offset(x = offset, y = offset)
            )
        }
    }
}

@Composable
private fun ChannelLock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = dimensions().spacing3x,
    size: Dp = dimensions().spacing14x,
) {
    Column(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color = colorsScheme().inverseSurface)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .padding(size / 7),
            tint = colorsScheme().inverseOnSurface
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewChannelPrivateConversationAvatar() {
    WireTheme {
        ChannelConversationAvatar(
            isPrivateChannel = true,
            conversationId = ConversationId("value", "domain"),
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewChannelConversationAvatar() {
    WireTheme {
        ChannelConversationAvatar(
            isPrivateChannel = false,
            conversationId = ConversationId("value", "domain")
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewChannelPrivateConversationAvatarSmall() = WireTheme {
    ChannelConversationAvatar(
        conversationId = ConversationId("value", "domain"),
        isPrivateChannel = true,
        size = dimensions().spacing18x,
        borderWidth = dimensions().spacing1x,
        cornerRadius = dimensions().spacing6x,
        padding = dimensions().spacing4x,
    )
}

@MultipleThemePreviews
@Composable
fun PreviewChannelLock() {
    WireTheme {
        ChannelLock()
    }
}
