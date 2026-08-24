/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.devices.remove

import com.wire.android.ui.authentication.devices.register.AuthenticationFailure

data class RemoveDeviceAuthenticationState<DeviceT>(
    val deviceList: List<DeviceT> = emptyList(),
    val removeDeviceDialogState: RemoveDeviceAuthenticationDialogState<DeviceT> =
        RemoveDeviceAuthenticationDialogState.Hidden,
    val isLoadingClientsList: Boolean = true,
    val error: RemoveDeviceAuthenticationError = RemoveDeviceAuthenticationError.None,
    val is2FAInProgress: Boolean = false,
)

sealed interface RemoveDeviceAuthenticationDialogState<out DeviceT> {
    data object Hidden : RemoveDeviceAuthenticationDialogState<Nothing>

    data class Visible<DeviceT>(
        val device: DeviceT,
        val loading: Boolean = false,
        val removeEnabled: Boolean = false,
    ) : RemoveDeviceAuthenticationDialogState<DeviceT>
}

sealed interface RemoveDeviceAuthenticationError {
    data object None : RemoveDeviceAuthenticationError
    data object InvalidCredentialsError : RemoveDeviceAuthenticationError
    data object InitError : RemoveDeviceAuthenticationError
    data class GenericError(val failure: AuthenticationFailure) : RemoveDeviceAuthenticationError
}
