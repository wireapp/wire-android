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
package com.wire.android.navigation

import androidx.compose.runtime.Composable
import com.wire.android.navigation.style.DialogNavigation
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.destinations.AvatarPickerScreenDestination
import com.wire.android.ui.destinations.ChangeDisplayNameScreenDestination
import com.wire.android.ui.destinations.ChangeEmailScreenDestination
import com.wire.android.ui.destinations.ChangeHandleScreenDestination
import com.wire.android.ui.destinations.EditConversationNameScreenDestination
import com.wire.android.ui.destinations.GroupConversationDetailsScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.destinations.EditGuestAccessScreenDestination
import com.wire.android.ui.destinations.EditSelfDeletingMessagesScreenDestination
import com.wire.android.ui.destinations.ChannelAccessOnUpdateScreenDestination
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.NewConversationFolderScreenDestination
import com.wire.android.ui.theme.isTablet

@Suppress("CyclomaticComplexMethod")
@Composable
fun AdjustDestinationStylesForTablets() {
    ServiceDetailsScreenDestination.style = if (isTablet) DialogNavigation else PopUpNavigationAnimation
    OtherUserProfileScreenDestination.style = if (isTablet) DialogNavigation else PopUpNavigationAnimation
    SelfUserProfileScreenDestination.style = if (isTablet) DialogNavigation else PopUpNavigationAnimation
    DeviceDetailsScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    ChangeDisplayNameScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    ChangeHandleScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    ChangeEmailScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    AvatarPickerScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    GroupConversationDetailsScreenDestination.style = if (isTablet) DialogNavigation else PopUpNavigationAnimation
    EditConversationNameScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    EditGuestAccessScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    ChannelAccessOnUpdateScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    EditSelfDeletingMessagesScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    ConversationFoldersScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
    NewConversationFolderScreenDestination.style = if (isTablet) DialogNavigation else SlideNavigationAnimation
}
