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
package com.wire.shared.auth.flow

import com.wire.shared.auth.AuthLoginSuccessPayload

data class AuthLoginFlowState(
    val step: AuthLoginFlowStep = AuthLoginFlowStep.IdentifierEntry,
    val identifier: String = "",
    val ssoCode: String = "",
    val password: String = "",
    val secondFactorCode: String = "",
    val secondFactorEmail: String = "",
    val isLoading: Boolean = false,
    val error: AuthLoginFlowError? = null,
) {
    val canSubmitIdentifier: Boolean
        get() = identifier.isNotBlank() && !isLoading

    val canSubmitSsoCode: Boolean
        get() = ssoCode.isNotBlank() && !isLoading

    val canSubmitCredentials: Boolean
        get() = identifier.isNotBlank() && password.isNotBlank() && !isLoading

    val canSubmitSecondFactor: Boolean
        get() = secondFactorCode.isNotBlank() && !isLoading

    val isSuccess: Boolean
        get() = step is AuthLoginFlowStep.Success
}

sealed interface AuthLoginFlowStep {
    data object IdentifierEntry : AuthLoginFlowStep
    data object EmailCredentialsEntry : AuthLoginFlowStep
    data object SecondFactorEntry : AuthLoginFlowStep
    data class Success(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
    ) : AuthLoginFlowStep
}

sealed interface AuthLoginFlowError {
    data object InvalidIdentifier : AuthLoginFlowError
    data object InvalidCredentials : AuthLoginFlowError
    data object InvalidSecondFactorCode : AuthLoginFlowError
    data object TooManyDevices : AuthLoginFlowError
    data class Generic(val message: String? = null) : AuthLoginFlowError
}

sealed interface AuthLoginFlowIntent {
    data class IdentifierChanged(val value: String) : AuthLoginFlowIntent
    data class SsoCodeChanged(val value: String) : AuthLoginFlowIntent
    data object SubmitIdentifier : AuthLoginFlowIntent
    data object SubmitSsoCode : AuthLoginFlowIntent
    data class PasswordChanged(val value: String) : AuthLoginFlowIntent
    data class SubmitCredentials(val usernameAllowed: Boolean = true) : AuthLoginFlowIntent
    data class SecondFactorCodeChanged(val value: String) : AuthLoginFlowIntent
    data class SubmitSecondFactor(val usernameAllowed: Boolean = true) : AuthLoginFlowIntent
    data object Back : AuthLoginFlowIntent
    data object Cancel : AuthLoginFlowIntent
    data object ClearError : AuthLoginFlowIntent
}

sealed interface AuthLoginFlowEffect {
    data class OpenSsoUrl(
        val url: String,
        val userIdentifier: String,
    ) : AuthLoginFlowEffect

    data class LoginSucceeded(
        val payload: AuthLoginSuccessPayload,
    ) : AuthLoginFlowEffect
}
