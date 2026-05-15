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
package com.wire.ios.shared.auth.sso

import com.wire.ios.shared.auth.login.model.LoginServerLinks

/**
 * Swift-facing state for the SSO login screen.
 *
 * This mirrors the Android SSO screen state without exposing Compose, Android, or Kalium UI types.
 */
data class LoginSsoState(
    val ssoCode: String = "",
    val loginEnabled: Boolean = false,
    val flowState: LoginSsoFlowState = LoginSsoFlowState.Default,
    val customServerDialogState: LoginSsoCustomServerDialogState? = null,
)

sealed interface LoginSsoFlowState {
    data object Default : LoginSsoFlowState
    data object Loading : LoginSsoFlowState
    data object Canceled : LoginSsoFlowState
    data class Success(
        val initialSyncCompleted: Boolean,
        val e2eiRequired: Boolean,
    ) : LoginSsoFlowState
    data class Error(val reason: LoginSsoError) : LoginSsoFlowState
}

sealed interface LoginSsoError {
    data object InvalidValue : LoginSsoError
    data object InvalidSsoCode : LoginSsoError
    data object InvalidSsoCookie : LoginSsoError
    data object InvalidCredentials : LoginSsoError
    data object ProxyError : LoginSsoError
    data object UserAlreadyExists : LoginSsoError
    data object PasswordNeededToRegisterClient : LoginSsoError
    data object RequestTwoFactorAuthenticationWithHandle : LoginSsoError
    data object ServerVersionNotSupported : LoginSsoError
    data object ClientUpdateRequired : LoginSsoError
    data object AccountSuspended : LoginSsoError
    data object AccountPendingActivation : LoginSsoError
    data object TooManyDevices : LoginSsoError
    data class SsoResultError(val code: String) : LoginSsoError
    data class GenericError(val message: String? = null) : LoginSsoError
}

sealed interface LoginSsoIntent {
    data class SsoCodeChanged(val ssoCode: String) : LoginSsoIntent
    data object SubmitLogin : LoginSsoIntent
    data object ClearLoginErrors : LoginSsoIntent
    data object DismissCustomServerDialog : LoginSsoIntent
    data object ConfirmCustomServerDialog : LoginSsoIntent
    data class AutoFillSsoCode(
        val ssoCode: String,
        val autoInitiateLogin: Boolean,
        val nomadServiceUrl: String? = null,
        val cookieLabel: String? = null,
    ) : LoginSsoIntent
    data class CompleteSsoLogin(
        val cookie: String,
        val serverConfigId: String,
    ) : LoginSsoIntent
    data class ReportSsoLoginFailure(val code: String) : LoginSsoIntent
}

sealed interface LoginSsoEffect {
    /**
     * The platform layer should open [url] using an iOS gateway and route the callback back as an intent.
     */
    data class OpenUrl(
        val url: String,
        val serverLinks: LoginServerLinks,
    ) : LoginSsoEffect
}

data class LoginSsoCustomServerDialogState(
    val serverLinks: LoginServerLinks,
)
