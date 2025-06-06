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

package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.groupConversationColor
import com.wire.android.ui.home.conversations.info.ConversationAvatar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId

@Composable
fun GroupConversationAvatar(
    avatarData: ConversationAvatar.Group,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize,
    cornerRadius: Dp = MaterialTheme.wireDimensions.groupAvatarCornerRadius,
    padding: Dp = MaterialTheme.wireDimensions.avatarClickablePadding,
    borderWidth: Dp = dimensions().avatarBorderWidth,
    borderColor: Color = colorsScheme().outline
) = when (avatarData) {
    is ConversationAvatar.Group.Channel -> ChannelConversationAvatar(
        avatarData.conversationId,
        avatarData.isPrivate,
        modifier,
        size,
        cornerRadius,
        padding,
        borderWidth
    )

    is ConversationAvatar.Group.Regular -> RegularGroupConversationAvatar(
        avatarData.conversationId,
        modifier,
        size,
        cornerRadius,
        padding,
        borderWidth,
        borderColor
    )
}

@Composable
fun RegularGroupConversationAvatar(
    conversationId: ConversationId,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize,
    cornerRadius: Dp = MaterialTheme.wireDimensions.groupAvatarCornerRadius,
    padding: Dp = MaterialTheme.wireDimensions.avatarClickablePadding,
    borderWidth: Dp = dimensions().avatarBorderWidth,
    borderColor: Color = colorsScheme().outline
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(padding)
            .size(size + borderWidth * 2) // border exceeds the size to keep sizes consistent with UserProfileAvatar
            .border(color = borderColor, width = borderWidth, shape = RoundedCornerShape(cornerRadius + borderWidth))
            .padding(borderWidth)
            .background(color = colorsScheme().surface, shape = RoundedCornerShape(cornerRadius))
    ) {
        val colors = colorsScheme().groupConversationColor(id = conversationId)
        CustomGroupAvatarDrawing(
            modifier = Modifier.padding(dimensions().spacing4x),
            leftSideShapeColor = colors.left,
            middleSideShapeColor = colors.middle,
            rightSideShapeColor = colors.right
        )
    }
}

@Preview
@Composable
fun PreviewGroupConversationAvatar() {
    WireTheme {
        RegularGroupConversationAvatar(
            conversationId = ConversationId("conversationId", "domain")
        )
    }
}
