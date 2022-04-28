package com.wire.android.ui.authentication.login.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.login.LoginError

data class LoginEmailState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginEmailError: LoginError = LoginError.None
)
