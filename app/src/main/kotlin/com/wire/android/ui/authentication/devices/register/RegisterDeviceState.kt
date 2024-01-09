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

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class RegisterDeviceState(
    val password: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val flowState: RegisterDeviceFlowState = RegisterDeviceFlowState.Default
)
sealed class RegisterDeviceFlowState {
    object Default : RegisterDeviceFlowState()
    object Loading : RegisterDeviceFlowState()
    object TooManyDevices : RegisterDeviceFlowState()
    data class Success(val initialSyncCompleted: Boolean) : RegisterDeviceFlowState()
    sealed class Error : RegisterDeviceFlowState() {
        object InvalidCredentialsError : Error()
        data class GenericError(val coreFailure: CoreFailure) : Error()
    }
}
