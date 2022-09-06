package com.wire.android.ui.authentication.login

import androidx.compose.ui.text.input.TextFieldValue

data class LoginState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val ssoLoginLoading: Boolean = false,
    val emailLoginLoading: Boolean = false,
    val ssoLoginEnabled: Boolean = false,
    val emailLoginEnabled: Boolean = false,
    val loginError: LoginError = LoginError.None,
    val showClientUpdateDialog: Boolean = false,
    val showServerVersionNotSupportedDialog: Boolean = false
)

fun LoginState.updateEmailLoginEnabled() =
    copy(emailLoginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !emailLoginLoading)

fun LoginState.updateSSOLoginEnabled() =
    copy(ssoLoginEnabled = ssoCode.text.isNotEmpty() && !ssoLoginLoading)

