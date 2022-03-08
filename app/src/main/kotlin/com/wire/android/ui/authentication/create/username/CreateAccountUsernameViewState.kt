package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.CreateAccountFlowType

data class CreateAccountUsernameViewState(
    val type: CreateAccountFlowType,
    val username: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: UsernameError = UsernameError.None
) {
    sealed class UsernameError {
        object None : UsernameError()
        object UsernameTakenError : UsernameError()
    }
}
