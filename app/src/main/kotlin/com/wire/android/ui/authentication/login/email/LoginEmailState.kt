package com.wire.android.ui.authentication.login.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class LoginEmailState(
    val userIdentifier: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginEmailError: LoginEmailError = LoginEmailError.None
)

sealed class LoginEmailError {
    object None: LoginEmailError()
    sealed class TextFieldError: LoginEmailError() {
        object InvalidUserIdentifierError: TextFieldError()
    }
    sealed class DialogError: LoginEmailError() {
        object InvalidCredentialsError: DialogError()
        object UserAlreadyExists: DialogError()
        data class GenericError(val coreFailure: CoreFailure): DialogError()
    }
    object TooManyDevicesError: LoginEmailError()
}
