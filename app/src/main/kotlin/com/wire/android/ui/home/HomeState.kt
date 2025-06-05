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

import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.ui.destinations.CreateAccountUsernameScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.RegisterDeviceScreenDestination

data class HomeState(
    val userAvatarData: UserAvatarData = UserAvatarData(null),
    val shouldDisplayLegalHoldIndicator: Boolean = false,
    val shouldShowCreateTeamUnreadIndicator: Boolean = false,
)

sealed class HomeRequirement {
    data object RegisterDevice : HomeRequirement()
    data object CreateAccountUsername : HomeRequirement()
    data object InitialSync : HomeRequirement()

    fun navigate(navigate: (NavigationCommand) -> Unit) = when (this) {
        is RegisterDevice -> navigate(NavigationCommand(RegisterDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
        is CreateAccountUsername -> navigate(NavigationCommand(CreateAccountUsernameScreenDestination, BackStackMode.CLEAR_WHOLE))
        is InitialSync -> navigate(NavigationCommand(InitialSyncScreenDestination, BackStackMode.CLEAR_WHOLE))
    }
}
