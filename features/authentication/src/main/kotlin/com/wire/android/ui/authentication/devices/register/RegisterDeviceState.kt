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

package com.wire.android.ui.authentication.devices.register

data class RegisterDeviceState<SessionT>(
    val continueEnabled: Boolean = false,
    val flowState: RegisterDeviceFlowState<SessionT> = RegisterDeviceFlowState.Default
)

sealed class RegisterDeviceFlowState<out SessionT> {
    data object Default : RegisterDeviceFlowState<Nothing>()
    data object Loading : RegisterDeviceFlowState<Nothing>()
    data object TooManyDevices : RegisterDeviceFlowState<Nothing>()
    data class Success<SessionT>(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
        val e2eiSessionId: SessionT? = null,
    ) : RegisterDeviceFlowState<SessionT>()

    sealed class Error : RegisterDeviceFlowState<Nothing>() {
        data object InvalidCredentialsError : Error()
        data class GenericError(val failure: AuthenticationFailure) : Error()
    }
}
