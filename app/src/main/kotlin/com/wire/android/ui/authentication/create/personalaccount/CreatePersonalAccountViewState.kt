package com.wire.android.ui.authentication.create.personalaccount

import androidx.compose.ui.text.input.TextFieldValue

data class CreatePersonalAccountViewState(
    val email: Email = Email(),
    val details: Details = Details(),
) {
    data class Email(
        val email: TextFieldValue = TextFieldValue(""),
        val termsDialogVisible: Boolean = false,
        val continueEnabled: Boolean = false,
        val loading: Boolean = false,
        val error: EmailError = EmailError.None
    )
    sealed class EmailError {
        object None : EmailError()
        object InvalidEmailError: EmailError()
    }

    data class Details(
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
    }
    sealed class DetailsError {
        object None : DetailsError()
        object InvalidPasswordError: DetailsError()
        object PasswordsNotMatchingError: DetailsError()
    }
}

