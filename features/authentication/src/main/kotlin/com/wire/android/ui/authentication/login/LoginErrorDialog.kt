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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.wireDialogPropertiesBuilder

enum class LoginDialogType {
    InvalidCredentials,
    UserAlreadyExists,
    ProxyError,
    InvalidSsoCode,
    InvalidSsoCookie,
    RequestTwoFactorWithHandle,
    AccountSuspended,
    AccountPendingActivation,
}

sealed interface LoginDialogErrorData {
    val actionText: String
    val onAction: (() -> Unit)?
    val dismissOnClickOutside: Boolean

    @Immutable
    data class Known(
        val type: LoginDialogType,
        override val actionText: String,
        override val onAction: (() -> Unit)? = null,
        override val dismissOnClickOutside: Boolean = true,
    ) : LoginDialogErrorData

    @Immutable
    data class SsoResult(
        val errorCode: Int,
        override val actionText: String,
        override val onAction: (() -> Unit)? = null,
        override val dismissOnClickOutside: Boolean = true,
    ) : LoginDialogErrorData

    @Immutable
    data class Resolved(
        val title: String,
        val body: AnnotatedString,
        override val actionText: String,
        override val onAction: (() -> Unit)? = null,
        override val dismissOnClickOutside: Boolean = true,
    ) : LoginDialogErrorData
}

@Composable
fun LoginErrorDialog(
    dialogErrorData: LoginDialogErrorData,
    onDialogDismiss: () -> Unit,
) {
    val (title, body) = dialogErrorData.resolveText()
    WireDialog(
        title = title,
        text = body,
        onDismiss = onDialogDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = dialogErrorData.actionText,
            onClick = dialogErrorData.onAction ?: onDialogDismiss,
            type = WireDialogButtonType.Primary,
        ),
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = true,
            dismissOnClickOutside = dialogErrorData.dismissOnClickOutside,
        ),
    )
}

@Composable
private fun LoginDialogErrorData.resolveText(): Pair<String, AnnotatedString> = when (this) {
    is LoginDialogErrorData.Resolved -> title to body
    is LoginDialogErrorData.SsoResult -> stringResource(R.string.sso_error_dialog_title) to
            AnnotatedString(stringResource(R.string.sso_error_dialog_message, errorCode))
    is LoginDialogErrorData.Known -> type.resolveText()
}

@Composable
private fun LoginDialogType.resolveText(): Pair<String, AnnotatedString> {
    val resources = when (this) {
        LoginDialogType.InvalidCredentials ->
            R.string.login_error_invalid_credentials_title to R.string.login_error_invalid_credentials_message
        LoginDialogType.UserAlreadyExists ->
            R.string.login_error_user_already_logged_in_title to R.string.login_error_user_already_logged_in_message
        LoginDialogType.ProxyError -> R.string.error_socket_title to R.string.error_socket_message
        LoginDialogType.InvalidSsoCode ->
            R.string.login_error_invalid_credentials_title to R.string.login_error_invalid_sso_code
        LoginDialogType.InvalidSsoCookie ->
            R.string.login_sso_error_invalid_cookie_title to R.string.login_sso_error_invalid_cookie_message
        LoginDialogType.RequestTwoFactorWithHandle ->
            R.string.login_error_request_2fa_with_handle_title to R.string.login_error_request_2fa_with_handle_message
        LoginDialogType.AccountSuspended ->
            R.string.login_error_unauthorized_title to R.string.login_error_unauthorized_message
        LoginDialogType.AccountPendingActivation ->
            R.string.login_error_pending_activation_title to R.string.login_error_pending_activation_message
    }
    return stringResource(resources.first) to AnnotatedString(stringResource(resources.second))
}
