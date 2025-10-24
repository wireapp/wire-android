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
package com.wire.android.ui.common.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun UserProfileAvatarsRow(
    avatars: List<UserAvatarData>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = dimensions().spacing16x,
    overlapSize: Dp = dimensions().spacing8x,
    borderWidth: Dp = dimensions().spacing1x,
    borderColor: Color = colorsScheme().surfaceVariant,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-overlapSize)
    ) {
        avatars.forEach { avatarData ->
            UserProfileAvatar(
                avatarData = avatarData,
                size = avatarSize,
                avatarBorderWidth = borderWidth,
                avatarBorderColor = borderColor,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(borderColor),
                padding = dimensions().spacing0x,
                type = UserProfileAvatarType.WithoutIndicators,
            )
        }
    }
}

private val mockAvatars = listOf(
    UserAvatarData(nameBasedAvatar = NameBasedAvatar("Alice", -1)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar("Bob", -1)),
    UserAvatarData(),
    UserAvatarData(nameBasedAvatar = NameBasedAvatar("Charlie", -1)),
)

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarsRow_TypingIndicator() = WireTheme {
    Surface(color = colorsScheme().surfaceVariant) {
        UserProfileAvatarsRow(
            avatars = mockAvatars,
            avatarSize = dimensions().spacing16x,
            overlapSize = dimensions().spacing8x,
            borderWidth = dimensions().spacing1x,
            borderColor = colorsScheme().surfaceVariant,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewUserProfileAvatarsRow_Meeting() = WireTheme {
    Surface(color = colorsScheme().surface) {
        UserProfileAvatarsRow(
            avatars = mockAvatars,
            avatarSize = dimensions().spacing18x,
            overlapSize = dimensions().spacing4x,
            borderWidth = dimensions().spacing1x,
            borderColor = colorsScheme().outline,
        )
    }
}
