package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue

data class EmailViewState(
    val email: TextFieldValue = TextFieldValue(""),
    val termsDialogVisible: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: EmailError = EmailError.None
) {
    sealed class EmailError {
        object None : EmailError()
        object InvalidEmailError: EmailError()
    }

}
