package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue

data class CreateAccountDetailsViewState(
    val firstName: TextFieldValue = TextFieldValue(""),
    val lastName: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val confirmPassword: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: DetailsError = DetailsError.None
) {
    fun fieldsNotEmpty(): Boolean =
        firstName.text.isNotEmpty() && lastName.text.isNotEmpty() && password.text.isNotEmpty() && confirmPassword.text.isNotEmpty()

    sealed class DetailsError {
        object None : DetailsError()
        object InvalidPasswordError: DetailsError()
        object PasswordsNotMatchingError: DetailsError()
    }
}
