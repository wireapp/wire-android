package com.wire.android.ui.authentication.login

import androidx.compose.ui.text.input.TextFieldValue

data class LoginState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val proxyIdentifier: TextFieldValue = TextFieldValue(""),
    val proxyPassword: TextFieldValue = TextFieldValue(""),
    val ssoLoginLoading: Boolean = false,
    val emailLoginLoading: Boolean = false,
    val ssoLoginEnabled: Boolean = false,
    val emailLoginEnabled: Boolean = false,
    val isProxyAuthRequired: Boolean = false,
    val loginError: LoginError = LoginError.None,
    val isProxyEnabled: Boolean = false
)

fun LoginState.updateEmailLoginEnabled() =
    copy(
        emailLoginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !emailLoginLoading &&
                (!isProxyAuthRequired || (isProxyAuthRequired && proxyIdentifier.text.isNotEmpty() && proxyPassword.text.isNotEmpty()))
    )

fun LoginState.updateSSOLoginEnabled() =
    copy(ssoLoginEnabled = ssoCode.text.isNotEmpty() && !ssoLoginLoading)

