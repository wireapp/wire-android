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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions

@Composable
fun GroupConversationAvatar(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.wireDimensions.avatarDefaultSize,
    cornerRadius: Dp = MaterialTheme.wireDimensions.groupAvatarCornerRadius,
    padding: Dp = MaterialTheme.wireDimensions.avatarClickablePadding,
    borderWidth: Dp = dimensions().avatarBorderWidth,
    borderColor: Color = colorsScheme().outline,
) {
    Box(
        modifier = modifier
            .padding(padding)
            .size(size + borderWidth * 2) // border exceeds the size to keep sizes consistent with UserProfileAvatar
            .border(color = borderColor, width = borderWidth, shape = RoundedCornerShape(cornerRadius + borderWidth))
            .padding(borderWidth)
            .background(color = color, shape = RoundedCornerShape(cornerRadius))
    )
}

@Preview
@Composable
fun PreviewGroupConversationAvatar() {
    GroupConversationAvatar(color = Color.Blue)
}
