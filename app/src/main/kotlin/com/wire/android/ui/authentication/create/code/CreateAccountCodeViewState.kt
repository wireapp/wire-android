package com.wire.android.ui.authentication.create.code

import androidx.compose.ui.text.input.TextFieldValue

data class CreateAccountCodeViewState(
    val code: TextFieldValue = TextFieldValue(""),
    val email: String = "",
    val loading: Boolean = false,
    val inputEnabled: Boolean = true,
    val error: CodeError = CodeError.None
) {
    sealed class CodeError {
        object None : CodeError()
        object InvalidCodeError: CodeError()
    }
}
