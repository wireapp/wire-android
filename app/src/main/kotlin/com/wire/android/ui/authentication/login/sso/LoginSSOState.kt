package com.wire.android.ui.authentication.login.sso

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class LoginSSOState(
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginSSOError: LoginSSOError = LoginSSOError.None
)

sealed class LoginSSOError {
    object None: LoginSSOError()
    sealed class TextFieldError: LoginSSOError() {
        object InvalidCodeFormatError: TextFieldError()
    }
    sealed class DialogError: LoginSSOError() {
        object InvalidCodeError: DialogError()
        data class GenericError(val coreFailure: CoreFailure): DialogError()
    }
}

