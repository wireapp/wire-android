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

package com.wire.android.ui.authentication.devices.remove

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.common.error.CoreFailure

data class RemoveDeviceState(
    val deviceList: List<Device>,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val isLoadingClientsList: Boolean,
    val error: RemoveDeviceError = RemoveDeviceError.None,
    val is2FAInProgress: Boolean = false,
)

sealed class RemoveDeviceDialogState {
    data object Hidden : RemoveDeviceDialogState()
    data class Visible(
        val device: Device,
        val loading: Boolean = false,
        val removeEnabled: Boolean = false
    ) : RemoveDeviceDialogState()
}

sealed class RemoveDeviceError {
    data object None : RemoveDeviceError()
    data object InvalidCredentialsError : RemoveDeviceError()
    data object InitError : RemoveDeviceError()
    data class GenericError(val coreFailure: CoreFailure) : RemoveDeviceError()
}
