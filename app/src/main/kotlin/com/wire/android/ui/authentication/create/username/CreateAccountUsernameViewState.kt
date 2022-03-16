package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue

data class CreateAccountUsernameViewState(
    val username: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: UsernameError = UsernameError.None
) {
    sealed class UsernameError {
        object None : UsernameError()
        object UsernameTakenError : UsernameError()
        object UsernameInvalidError : UsernameError()
    }
}
