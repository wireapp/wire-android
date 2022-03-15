package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountUsernameFlowType

data class CreateAccountUsernameViewState(
    val type: CreateAccountUsernameFlowType,
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
