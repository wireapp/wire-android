package com.wire.android.ui.authentication.login

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class LoginState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginError: LoginError = LoginError.None
)

sealed class LoginError {
    object None: LoginError()
    sealed class TextFieldError: LoginError() {
        object InvalidUserIdentifierError: TextFieldError()
    }
    sealed class DialogError: LoginError() {
        object InvalidCredentialsError: DialogError()
        data class GenericError(val coreFailure: CoreFailure): DialogError()
    }
}
