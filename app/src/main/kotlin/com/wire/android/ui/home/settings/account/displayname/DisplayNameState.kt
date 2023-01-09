package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.ui.text.input.TextFieldValue

data class DisplayNameState(
    val originalDisplayName: String = "",
    val displayName: TextFieldValue = TextFieldValue(""),
    val error: NameError = NameError.None,
    val animatedNameError: Boolean = false,
    val continueEnabled: Boolean = false
) {
    sealed interface NameError {
        object None : NameError
        sealed interface TextFieldError : NameError {
            object NameEmptyError : TextFieldError
            object NameExceedLimitError : TextFieldError
        }
    }
}
