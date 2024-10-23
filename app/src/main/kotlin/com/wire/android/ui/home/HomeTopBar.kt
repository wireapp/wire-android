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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.UserProfileAvatarType
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun HomeTopBar(
    userAvatarData: UserAvatarData,
    title: String,
    elevation: Dp,
    withLegalHoldIndicator: Boolean,
    onHamburgerMenuClick: () -> Unit,
    onNavigateToSelfUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = title,
        titleContentDescription = stringResource(R.string.content_description_home_header),
        onNavigationPressed = onHamburgerMenuClick,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            val openLabel = stringResource(R.string.content_description_open_label)
            UserProfileAvatar(
                avatarData = userAvatarData,
                clickable = remember {
                    Clickable(enabled = true, onClickDescription = openLabel) { onNavigateToSelfUserProfile() }
                },
                type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = withLegalHoldIndicator),
                contentDescription = stringResource(R.string.content_description_home_profile_btn)
            )
        },
        elevation = elevation,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBar() {
    WireTheme {
        HomeTopBar(UserAvatarData(null, UserAvailabilityStatus.AVAILABLE), "Title", 0.dp, false, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithNameBasedAvatar() {
    WireTheme {
        HomeTopBar(
            UserAvatarData(
                asset = null,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            title = "Title",
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithLegalHold() {
    WireTheme {
        HomeTopBar(UserAvatarData(null, UserAvailabilityStatus.AVAILABLE), "Title", 0.dp, true, {}, {})
    }
}
