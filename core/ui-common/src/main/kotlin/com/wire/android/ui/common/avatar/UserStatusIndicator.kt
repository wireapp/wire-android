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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun UserStatusIndicator(
    status: UserAvailabilityStatus,
    modifier: Modifier = Modifier,
    size: Dp = dimensions().avatarStatusSize,
    borderWidth: Dp = dimensions().avatarStatusBorderWidth,
    borderColor: Color = Color.Transparent,
) {
    when (status) {
        UserAvailabilityStatus.AVAILABLE -> AvailableDot(modifier, size, borderWidth, borderColor)
        UserAvailabilityStatus.BUSY -> BusyDot(modifier, size, borderWidth, borderColor)
        UserAvailabilityStatus.AWAY -> AwayDot(modifier, size, borderWidth, borderColor)
        UserAvailabilityStatus.NONE -> {}
    }
}

@Composable
private fun AvailableDot(
    modifier: Modifier = Modifier,
    size: Dp = dimensions().avatarStatusSize,
    borderWidth: Dp = dimensions().avatarStatusBorderWidth,
    borderColor: Color = colorsScheme().background,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(borderColor, CircleShape)
            .padding(borderWidth)
            .background(MaterialTheme.wireColorScheme.positive, CircleShape)
    )
}

@Composable
private fun BusyDot(
    modifier: Modifier = Modifier,
    size: Dp = dimensions().avatarStatusSize,
    borderWidth: Dp = dimensions().avatarStatusBorderWidth,
    borderColor: Color = colorsScheme().background,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(borderColor, CircleShape)
            .padding(borderWidth)
            .background(MaterialTheme.wireColorScheme.warning, CircleShape)
            .padding(
                horizontal = size * 3 / 16,
                vertical = size * 5 / 16,
            )
            .background(borderColor)
    )
}

@Composable
private fun AwayDot(
    modifier: Modifier = Modifier,
    size: Dp = dimensions().avatarStatusSize,
    borderWidth: Dp = dimensions().avatarStatusBorderWidth,
    borderColor: Color = colorsScheme().background,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(borderColor, CircleShape)
            .padding(borderWidth)
            .background(MaterialTheme.wireColorScheme.error, CircleShape)
            .padding(size / 8)
            .background(borderColor, CircleShape)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAvailable() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().background)) {
        UserStatusIndicator(UserAvailabilityStatus.AVAILABLE, borderColor = colorsScheme().surfaceVariant)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewBusy() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().background)) {
        UserStatusIndicator(UserAvailabilityStatus.BUSY, borderColor = colorsScheme().surfaceVariant)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAway() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().background)) {
        UserStatusIndicator(UserAvailabilityStatus.AWAY, borderColor = colorsScheme().surfaceVariant)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNone() = WireTheme {
    Box(modifier = Modifier.background(colorsScheme().background)) {
        UserStatusIndicator(UserAvailabilityStatus.NONE, borderColor = colorsScheme().surfaceVariant)
    }
}
