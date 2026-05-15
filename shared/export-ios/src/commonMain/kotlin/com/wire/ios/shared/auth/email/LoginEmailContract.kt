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
package com.wire.ios.shared.auth.email

data class LoginEmailState(
    val userIdentifier: String = "",
    val password: String = "",
    val proxyIdentifier: String = "",
    val proxyPassword: String = "",
    val userIdentifierEnabled: Boolean = true,
    val loginEnabled: Boolean = false,
    val flowState: LoginEmailFlowState = LoginEmailFlowState.Default,
    val secondFactorVerificationCode: LoginEmailVerificationCodeState = LoginEmailVerificationCodeState(),
)

data class LoginEmailVerificationCodeState(
    val code: String = "",
    val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH,
    val emailUsed: String = "",
    val isCodeInputNecessary: Boolean = false,
    val isCurrentCodeInvalid: Boolean = false,
    val remainingTimerText: String? = null,
) {
    companion object {
        const val DEFAULT_VERIFICATION_CODE_LENGTH = 6
    }
}

sealed interface LoginEmailFlowState {
    data object Default : LoginEmailFlowState
    data object Loading : LoginEmailFlowState
    data object Canceled : LoginEmailFlowState
    data class Success(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
    ) : LoginEmailFlowState
    data class Error(val type: LoginEmailError) : LoginEmailFlowState
}

sealed interface LoginEmailError {
    data object InvalidUserIdentifier : LoginEmailError
    data object InvalidCredentials : LoginEmailError
    data object ProxyAuthenticationFailed : LoginEmailError
    data object UserAlreadyExists : LoginEmailError
    data object PasswordNeededToRegisterClient : LoginEmailError
    data object RequestSecondFactorWithHandle : LoginEmailError
    data object ServerVersionNotSupported : LoginEmailError
    data object ClientUpdateRequired : LoginEmailError
    data object AccountSuspended : LoginEmailError
    data object AccountPendingActivation : LoginEmailError
    data object TooManyDevices : LoginEmailError
    data class Generic(val message: String? = null) : LoginEmailError
}

sealed interface LoginEmailIntent {
    data class UserIdentifierChanged(val value: String) : LoginEmailIntent
    data class PasswordChanged(val value: String) : LoginEmailIntent
    data class ProxyIdentifierChanged(val value: String) : LoginEmailIntent
    data class ProxyPasswordChanged(val value: String) : LoginEmailIntent
    data class SecondFactorCodeChanged(val value: String) : LoginEmailIntent
    data class SubmitLogin(val usernameAllowed: Boolean = true) : LoginEmailIntent
    data object ClearLoginErrors : LoginEmailIntent
    data object CancelLogin : LoginEmailIntent
    data object SecondFactorBackPressed : LoginEmailIntent
    data object ResendSecondFactorCode : LoginEmailIntent
}

sealed interface LoginEmailEffect {
    data class LoginSucceeded(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
    ) : LoginEmailEffect
    data object RemoveDeviceNeeded : LoginEmailEffect
}
