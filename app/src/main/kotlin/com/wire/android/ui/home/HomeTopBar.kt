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
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.feature.cells.domain.model.CellsFilter
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Composable
fun HomeTopBar(
    title: String,
    currentConversationFilter: ConversationFilter,
    currentCellsFilters: Set<CellsFilter>,
    navigationItem: HomeDestination,
    userAvatarData: UserAvatarData,
    elevation: Dp,
    withLegalHoldIndicator: Boolean,
    shouldShowCreateTeamUnreadIndicator: Boolean,
    onHamburgerMenuClick: () -> Unit,
    onNavigateToSelfUserProfile: () -> Unit,
    onOpenConversationFilter: (conversationFilter: ConversationFilter) -> Unit,
    onOpenFilesFilter: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        title = title,
        onNavigationPressed = onHamburgerMenuClick,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            if (navigationItem.withNewConversationFab) {
                WireTertiaryIconButton(
                    iconResource = com.wire.android.ui.common.R.drawable.ic_filter,
                    contentDescription = R.string.label_filter_conversations,
                    state = if (currentConversationFilter == ConversationFilter.All) {
                        WireButtonState.Default
                    } else {
                        WireButtonState.Selected
                    },
                    onButtonClicked = { onOpenConversationFilter(currentConversationFilter) }
                )
            }
            if (navigationItem.withFilesFilterIcon) {
                WireTertiaryIconButton(
                    iconResource = com.wire.android.ui.common.R.drawable.ic_filter,
                    contentDescription = R.string.content_description_filter_files,
                    state = if (currentCellsFilters.isEmpty()) {
                        WireButtonState.Default
                    } else {
                        WireButtonState.Selected
                    },
                    onButtonClicked = { onOpenFilesFilter() }
                )
            }
            if (navigationItem.withUserAvatar) {
                val openLabel = stringResource(R.string.content_description_open_label)
                val contentDescription = if (shouldShowCreateTeamUnreadIndicator) {
                    stringResource(R.string.content_description_home_profile_btn_with_notification)
                } else {
                    stringResource(R.string.content_description_home_profile_btn)
                }
                UserProfileAvatar(
                    avatarData = userAvatarData,
                    clickable = remember {
                        Clickable(
                            enabled = true,
                            onClickDescription = openLabel
                        ) { onNavigateToSelfUserProfile() }
                    },
                    type = UserProfileAvatarType.WithIndicators.RegularUser(
                        legalHoldIndicatorVisible = withLegalHoldIndicator
                    ),
                    shouldShowCreateTeamUnreadIndicator = shouldShowCreateTeamUnreadIndicator,
                    contentDescription = contentDescription
                )
            }
        },
        elevation = elevation,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBar() {
    WireTheme {
        HomeTopBar(
            title = "Conversations",
            navigationItem = HomeDestination.Conversations,
            currentConversationFilter = ConversationFilter.All,
            currentCellsFilters = setOf(),
            userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {},
            onOpenConversationFilter = {},
            onOpenFilesFilter = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithSelectedFilter() {
    WireTheme {
        HomeTopBar(
            title = "Conversations",
            currentConversationFilter = ConversationFilter.Groups,
            currentCellsFilters = setOf(),
            navigationItem = HomeDestination.Conversations,
            userAvatarData = UserAvatarData(
                asset = null,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {},
            onOpenConversationFilter = {},
            onOpenFilesFilter = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSettingsTopBarWithoutAvatar() {
    WireTheme {
        HomeTopBar(
            title = "Settings",
            navigationItem = HomeDestination.Settings,
            currentConversationFilter = ConversationFilter.All,
            currentCellsFilters = setOf(),
            userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {},
            onOpenConversationFilter = {},
            onOpenFilesFilter = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithNameBasedAvatar() {
    WireTheme {
        HomeTopBar(
            title = "Conversations",
            navigationItem = HomeDestination.Conversations,
            currentConversationFilter = ConversationFilter.All,
            currentCellsFilters = setOf(),
            userAvatarData = UserAvatarData(
                asset = null,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                nameBasedAvatar = NameBasedAvatar("Jon Doe", -1)
            ),
            elevation = 0.dp,
            withLegalHoldIndicator = false,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {},
            onOpenConversationFilter = {},
            onOpenFilesFilter = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewTopBarWithLegalHold() {
    WireTheme {
        HomeTopBar(
            title = "Archive",
            navigationItem = HomeDestination.Archive,
            currentConversationFilter = ConversationFilter.All,
            currentCellsFilters = setOf(),
            userAvatarData = UserAvatarData(null, UserAvailabilityStatus.AVAILABLE),
            elevation = 0.dp,
            withLegalHoldIndicator = true,
            shouldShowCreateTeamUnreadIndicator = false,
            onHamburgerMenuClick = {},
            onNavigateToSelfUserProfile = {},
            onOpenConversationFilter = {},
            onOpenFilesFilter = {},
        )
    }
}
