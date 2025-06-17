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

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.newauthentication.login.NewLoginFlowState
import com.wire.android.ui.newauthentication.login.toLoginStateDialogError
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.launchUpdateTheApp

@Composable
fun LoginErrorDialog(dialogErrorData: LoginDialogErrorData, onDialogDismiss: () -> Unit) {
    WireDialog(
        title = dialogErrorData.title,
        text = dialogErrorData.body,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(dialogErrorData.actionTextId),
            onClick = dialogErrorData.onAction ?: onDialogDismiss,
            type = WireDialogButtonType.Primary
        ),
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = true,
            dismissOnClickOutside = dialogErrorData.dismissOnClickOutside,
        )
    )
}

@Composable
fun LoginState.Error.DialogError.toLoginDialogErrorData() = when (this) {
    is LoginState.Error.DialogError.InvalidCredentialsError -> LoginDialogErrorData(
        title = stringResource(R.string.login_error_invalid_credentials_title),
        body = AnnotatedString(stringResource(R.string.login_error_invalid_credentials_message)),
    )

    is LoginState.Error.DialogError.UserAlreadyExists -> LoginDialogErrorData(
        title = stringResource(R.string.login_error_user_already_logged_in_title),
        body = AnnotatedString(stringResource(R.string.login_error_user_already_logged_in_message)),
    )

    is LoginState.Error.DialogError.ProxyError -> LoginDialogErrorData(
        title = stringResource(R.string.error_socket_title),
        body = AnnotatedString(stringResource(R.string.error_socket_message)),
    )

    is LoginState.Error.DialogError.GenericError -> {
        val strings = this.coreFailure.dialogErrorStrings(LocalContext.current.resources)
        LoginDialogErrorData(
            title = strings.title,
            body = strings.annotatedMessage,
        )
    }

    is LoginState.Error.DialogError.InvalidSSOCodeError -> LoginDialogErrorData(
        title = stringResource(R.string.login_error_invalid_credentials_title),
        body = AnnotatedString(stringResource(R.string.login_error_invalid_sso_code)),
    )

    is LoginState.Error.DialogError.InvalidSSOCookie -> LoginDialogErrorData(
        title = stringResource(R.string.login_sso_error_invalid_cookie_title),
        body = AnnotatedString(stringResource(R.string.login_sso_error_invalid_cookie_message)),
    )

    is LoginState.Error.DialogError.SSOResultError -> LoginDialogErrorData(
        title = stringResource(R.string.sso_error_dialog_title),
        body = AnnotatedString(stringResource(R.string.sso_error_dialog_message, this.result.errorCode)),
    )

    is LoginState.Error.DialogError.ServerVersionNotSupported -> LoginDialogErrorData(
        title = stringResource(R.string.api_versioning_server_version_not_supported_title),
        body = AnnotatedString(stringResource(R.string.api_versioning_server_version_not_supported_message)),
        actionTextId = R.string.label_close,
        dismissOnClickOutside = false
    )

    is LoginState.Error.DialogError.ClientUpdateRequired -> {
        val context = LocalContext.current
        LoginDialogErrorData(
            title = stringResource(R.string.api_versioning_client_update_required_title),
            body = AnnotatedString(stringResource(R.string.api_versioning_client_update_required_message)),
            actionTextId = R.string.label_update,
            onAction = context::launchUpdateTheApp,
            dismissOnClickOutside = false
        )
    }

    LoginState.Error.DialogError.Request2FAWithHandle -> LoginDialogErrorData(
        title = stringResource(R.string.login_error_request_2fa_with_handle_title),
        body = AnnotatedString(stringResource(R.string.login_error_request_2fa_with_handle_message)),
    )

    LoginState.Error.DialogError.AccountSuspended -> {
        LoginDialogErrorData(
            title = stringResource(R.string.login_error_unauthorized_title),
            body = AnnotatedString(stringResource(R.string.login_error_unauthorized_message)),
        )
    }

    LoginState.Error.DialogError.AccountPendingActivation -> {
        LoginDialogErrorData(
            title = stringResource(R.string.login_error_pending_activation_title),
            body = AnnotatedString(stringResource(R.string.login_error_pending_activation_message)),
        )
    }

    else -> LoginDialogErrorData(
        title = stringResource(R.string.error_unknown_title),
        body = AnnotatedString(stringResource(R.string.error_unknown_message)),
    )
}

@Composable
fun NewLoginFlowState.Error.DialogError.toLoginDialogErrorData() = this.toLoginStateDialogError().toLoginDialogErrorData()

data class LoginDialogErrorData(
    val title: String,
    val body: AnnotatedString,
    @StringRes val actionTextId: Int = R.string.label_ok,
    val onAction: (() -> Unit)? = null,
    val dismissOnClickOutside: Boolean = true
)
