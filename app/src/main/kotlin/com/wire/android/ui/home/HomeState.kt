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

import com.wire.android.model.ImageAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.ui.destinations.CreateAccountUsernameScreenDestination
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.ui.destinations.RegisterDeviceScreenDestination
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

data class HomeState(
    val avatarAsset: ImageAsset.UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val shouldDisplayWelcomeMessage: Boolean = false
)

sealed class HomeRequirement {
    data class Migration(val userId: UserId) : HomeRequirement()
    object RegisterDevice : HomeRequirement()
    object CreateAccountUsername : HomeRequirement()
    object None : HomeRequirement()

    fun navigate(navigate: (NavigationCommand) -> Unit) = when (this) {
        is Migration -> navigate(NavigationCommand(MigrationScreenDestination(this.userId), BackStackMode.CLEAR_WHOLE))
        is RegisterDevice -> navigate(NavigationCommand(RegisterDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
        is CreateAccountUsername -> navigate(NavigationCommand(CreateAccountUsernameScreenDestination, BackStackMode.CLEAR_WHOLE))
        is None -> null
    }
}
