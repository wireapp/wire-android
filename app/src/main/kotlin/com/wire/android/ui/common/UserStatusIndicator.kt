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

package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun UserStatusIndicator(status: UserAvailabilityStatus, modifier: Modifier = Modifier) {
    when (status) {
        UserAvailabilityStatus.AVAILABLE -> AvailableDot(modifier)
        UserAvailabilityStatus.BUSY -> BusyDot(modifier)
        UserAvailabilityStatus.AWAY -> AwayDot(modifier)
        UserAvailabilityStatus.NONE -> {}
    }
}

@Composable
private fun AvailableDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(MaterialTheme.wireColorScheme.background, CircleShape)
            .padding(dimensions().avatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.positive, CircleShape)
    )
}

@Composable
private fun BusyDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(MaterialTheme.wireColorScheme.background, CircleShape)
            .padding(dimensions().avatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.warning, CircleShape)
            .padding(
                top = dimensions().userAvatarBusyVerticalPadding,
                bottom = dimensions().userAvatarBusyVerticalPadding,
                start = dimensions().userAvatarBusyHorizontalPadding,
                end = dimensions().userAvatarBusyHorizontalPadding
            )
            .background(MaterialTheme.wireColorScheme.background)
    )
}

@Composable
private fun AwayDot(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(dimensions().userAvatarStatusSize)
            .background(MaterialTheme.wireColorScheme.background, CircleShape)
            .padding(dimensions().avatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.error, CircleShape)
            .padding(dimensions().avatarStatusBorderSize)
            .background(MaterialTheme.wireColorScheme.background, CircleShape)
    )
}

@Preview(name = "AvailablePreview")
@Composable
fun PreviewAvailable() {
    UserStatusIndicator(UserAvailabilityStatus.AVAILABLE)
}

@Preview(name = "BusyPreview")
@Composable
fun PreviewBusy() {
    UserStatusIndicator(UserAvailabilityStatus.BUSY)
}

@Preview(name = "AwayPreview")
@Composable
fun PreviewAway() {
    UserStatusIndicator(UserAvailabilityStatus.AWAY)
}

@Preview(name = "NonePreview")
@Composable
fun PreviewNone() {
    UserStatusIndicator(UserAvailabilityStatus.NONE)
}
