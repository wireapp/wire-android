/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.newauthentication.login.AppNewLoginDialogError
import com.wire.android.ui.newauthentication.login.toLoginStateDialogError
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.launchUpdateTheApp
import com.wire.kalium.common.error.CoreFailure

@Composable
fun AppLoginDialogError.toLoginDialogErrorData(): LoginDialogErrorData {
    val ok = stringResource(R.string.label_ok)
    return when (this) {
        LoginState.Error.DialogError.InvalidCredentialsError -> known(LoginDialogType.InvalidCredentials, ok)
        LoginState.Error.DialogError.UserAlreadyExists -> known(LoginDialogType.UserAlreadyExists, ok)
        LoginState.Error.DialogError.ProxyError -> known(LoginDialogType.ProxyError, ok)
        is LoginState.Error.DialogError.GenericError<*> -> {
            val strings = (coreFailure as CoreFailure).dialogErrorStrings(LocalContext.current.resources)
            LoginDialogErrorData.Resolved(strings.title, strings.annotatedMessage, ok)
        }
        LoginState.Error.DialogError.InvalidSSOCodeError -> known(LoginDialogType.InvalidSsoCode, ok)
        LoginState.Error.DialogError.InvalidSSOCookie -> known(LoginDialogType.InvalidSsoCookie, ok)
        is LoginState.Error.DialogError.SSOResultError<*> -> LoginDialogErrorData.SsoResult(
            errorCode = (result as SSOFailureCodes).errorCode,
            actionText = ok,
        )
        LoginState.Error.DialogError.ServerVersionNotSupported -> LoginDialogErrorData.Resolved(
            title = stringResource(R.string.api_versioning_server_version_not_supported_title),
            body = AnnotatedString(stringResource(R.string.api_versioning_server_version_not_supported_message)),
            actionText = stringResource(R.string.label_close),
            dismissOnClickOutside = false,
        )
        LoginState.Error.DialogError.ClientUpdateRequired -> {
            val context = LocalContext.current
            LoginDialogErrorData.Resolved(
                title = stringResource(R.string.api_versioning_client_update_required_title),
                body = AnnotatedString(stringResource(R.string.api_versioning_client_update_required_message)),
                actionText = stringResource(R.string.label_update),
                onAction = context::launchUpdateTheApp,
                dismissOnClickOutside = false,
            )
        }
        LoginState.Error.DialogError.Request2FAWithHandle -> known(LoginDialogType.RequestTwoFactorWithHandle, ok)
        LoginState.Error.DialogError.AccountSuspended -> known(LoginDialogType.AccountSuspended, ok)
        LoginState.Error.DialogError.AccountPendingActivation -> known(LoginDialogType.AccountPendingActivation, ok)
        else -> LoginDialogErrorData.Resolved(
            title = stringResource(R.string.error_unknown_title),
            body = AnnotatedString(stringResource(R.string.error_unknown_message)),
            actionText = ok,
        )
    }
}

@Composable
fun AppNewLoginDialogError.toLoginDialogErrorData(): LoginDialogErrorData =
    toLoginStateDialogError().toLoginDialogErrorData()

private fun known(type: LoginDialogType, actionText: String) =
    LoginDialogErrorData.Known(type = type, actionText = actionText)
