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
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
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
    shouldShowCreateTeamUnreadIndicator: Boolean,
    onHamburgerMenuClick: () -> Unit,
    onNavigateToSelfUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = title,
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
<<<<<<< HEAD
                shouldShowCreateTeamUnreadIndicator = shouldShowCreateTeamUnreadIndicator
=======
                contentDescription = stringResource(R.string.content_description_home_profile_btn)
>>>>>>> 175d83b8d (feat: Add accessibility string to ConversationList [WPB-9789] (#3561))
            )
        },
        elevation = elevation,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBar() {
    WireTheme {
        HomeTopBar(
            userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
            title = "Title",
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithNameBasedAvatar() {
    WireTheme {
        HomeTopBar(
            userAvatarData = UserAvatarData(
                asset = null,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            title = "Title",
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithLegalHold() {
    WireTheme {
        HomeTopBar(
            userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
            title = "Title",
            elevation = 0.dp,
            withLegalHoldIndicator = true,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {}
        )
    }
}
