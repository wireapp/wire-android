package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.NetworkFailure

data class CreateAccountDetailsViewState(
    val type: CreateAccountFlowType,
    val firstName: TextFieldValue = TextFieldValue(""),
    val lastName: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val confirmPassword: TextFieldValue = TextFieldValue(""),
    val teamName: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: DetailsError = DetailsError.None
) {
    fun fieldsNotEmpty(): Boolean =
        firstName.text.isNotEmpty() && lastName.text.isNotEmpty() && password.text.isNotEmpty() && confirmPassword.text.isNotEmpty()
                && (type == CreateAccountFlowType.CreatePersonalAccount || teamName.text.isNotEmpty())

    sealed class DetailsError {
        object None : DetailsError()
        sealed class TextFieldError: DetailsError() {
            object InvalidPasswordError: TextFieldError()
            object PasswordsNotMatchingError: TextFieldError()
        }
        sealed class DialogError: DetailsError() {
            data class GenericError(val coreFailure: NetworkFailure): DialogError()
        }
    }
}
