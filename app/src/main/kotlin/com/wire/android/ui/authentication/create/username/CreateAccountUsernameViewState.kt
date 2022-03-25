package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class CreateAccountUsernameViewState(
    val username: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: UsernameError = UsernameError.None
) {
    sealed class UsernameError {
        object None : UsernameError()
        sealed class TextFieldError : UsernameError() {
            object UsernameTakenError : TextFieldError()
            object UsernameInvalidError : TextFieldError()
        }

        sealed class DialogError : UsernameError() {
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }
    }
}
