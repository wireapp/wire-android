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
package com.wire.shared.auth.login.model

data class LoginScreenState(
    val isThereActiveSession: Boolean = false,
    val userIdentifierEnabled: Boolean = true,
    val nextEnabled: Boolean = false,
    val flowState: LoginFlowState = LoginFlowState.Default,
)

sealed interface LoginFlowState {
    data object Default : LoginFlowState
    data object Loading : LoginFlowState
    data class CustomConfigDialog(val serverLinks: LoginServerLinks) : LoginFlowState
    data class Error(val error: LoginError) : LoginFlowState
}

sealed interface LoginError {
    data object InvalidValue : LoginError
    data object ServerVersionNotSupported : LoginError
    data object ClientUpdateRequired : LoginError
    data class SsoResultFailure(val result: LoginSsoFailure) : LoginError
    data object InvalidSsoCode : LoginError
    data object InvalidSsoCookie : LoginError
    data object UserAlreadyExists : LoginError
    data object GenericError : LoginError
}
