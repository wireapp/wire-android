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

package com.wire.android.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.model.Clickable
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun HomeTopBar(
    avatarAsset: UserAvatarAsset?,
    status: UserAvailabilityStatus,
    title: String,
    elevation: Dp,
    withLegalHoldIndicator: Boolean,
    onHamburgerMenuClick: () -> Unit,
    onNavigateToSelfUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = title,
        onNavigationPressed = onHamburgerMenuClick,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            UserProfileAvatar(
                avatarData = UserAvatarData(avatarAsset, status),
                clickable = remember { Clickable(enabled = true) { onNavigateToSelfUserProfile() } },
                withLegalHoldIndicator = withLegalHoldIndicator,
            )
        },
        elevation = elevation,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBar() {
    WireTheme {
        HomeTopBar(null, UserAvailabilityStatus.AVAILABLE, "Title", 0.dp, false, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithLegalHold() {
    WireTheme {
        HomeTopBar(null, UserAvailabilityStatus.AVAILABLE, "Title", 0.dp, true, {}, {})
    }
}
