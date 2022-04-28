package com.wire.android.ui.authentication.login.sso

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.login.LoginError

data class LoginSSOState(
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginSSOError: LoginError = LoginError.None
)
