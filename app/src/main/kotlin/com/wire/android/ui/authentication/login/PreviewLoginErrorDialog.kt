/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.login

import androidx.compose.runtime.Composable
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_InvalidCredentialsError() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.InvalidCredentialsError, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_UserAlreadyExists() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.UserAlreadyExists, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_ProxyError() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.ProxyError, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_GenericError_NoNetworkConnection() = WireTheme {
    val coreFailure = NetworkFailure.NoNetworkConnection(RuntimeException())
    LoginErrorDialog(LoginState.Error.DialogError.GenericError(coreFailure), {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_GenericError_ServerMiscommunication() = WireTheme {
    val coreFailure = NetworkFailure.ServerMiscommunication(RuntimeException())
    LoginErrorDialog(LoginState.Error.DialogError.GenericError(coreFailure), {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_GenericError_Other() = WireTheme {
    val coreFailure = CoreFailure.Unknown(RuntimeException())
    LoginErrorDialog(LoginState.Error.DialogError.GenericError(coreFailure), {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_InvalidSSOCodeError() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.InvalidSSOCodeError, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_InvalidSSOCookie() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.InvalidSSOCookie, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_SSOResultError() = WireTheme {
    val ssoLoginResult = DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.ServerError)
    LoginErrorDialog(LoginState.Error.DialogError.SSOResultError(ssoLoginResult.ssoError), {}, {}, ssoLoginResult)
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_ServerVersionNotSupported() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.ServerVersionNotSupported, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_ClientUpdateRequired() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.ClientUpdateRequired, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_Request2FAWithHandle() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.Request2FAWithHandle, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_PasswordNeededToRegisterClient() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.PasswordNeededToRegisterClient, {}, {})
}


@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_AccountSuspended() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.AccountSuspended, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginErrorDialog_AccountPendingActivation() = WireTheme {
    LoginErrorDialog(LoginState.Error.DialogError.AccountPendingActivation, {}, {})
}
