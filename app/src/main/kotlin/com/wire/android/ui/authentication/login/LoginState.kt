package com.wire.android.ui.authentication.login

import androidx.compose.ui.text.input.TextFieldValue

data class LoginState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginError: LoginError = LoginError.None
)

fun LoginState.updateLoginEnabled() =
    copy(loginEnabled = userIdentifier.text.isNotEmpty() && password.text.isNotEmpty() && !loading)

