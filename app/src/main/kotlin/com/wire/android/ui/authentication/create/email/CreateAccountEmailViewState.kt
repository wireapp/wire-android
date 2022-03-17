package com.wire.android.ui.authentication.create.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.NetworkFailure

data class CreateAccountEmailViewState(
    val type: CreateAccountFlowType,
    val email: TextFieldValue = TextFieldValue(""),
    val termsDialogVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: EmailError = EmailError.None
) {
    sealed class EmailError {
        object None : EmailError()
        sealed class TextFieldError: EmailError() {
            object InvalidEmailError: TextFieldError()
            object BlacklistedEmailError: TextFieldError()
            object AlreadyInUseError: TextFieldError()
            object DomainBlockedError: TextFieldError()
        }
        sealed class DialogError: EmailError() {
            data class GenericError(val coreFailure: NetworkFailure): DialogError()
        }
    }

}
