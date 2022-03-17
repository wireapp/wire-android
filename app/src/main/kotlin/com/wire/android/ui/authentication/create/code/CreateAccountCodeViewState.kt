package com.wire.android.ui.authentication.create.code

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.NetworkFailure

data class CreateAccountCodeViewState(
    val type: CreateAccountFlowType,
    val code: CodeFieldValue = CodeFieldValue(TextFieldValue(""), false),
    val email: String = "",
    val loading: Boolean = false,
    val inputEnabled: Boolean = true,
    val error: CodeError = CodeError.None
) {
    sealed class CodeError {
        object None : CodeError()
        sealed class TextFieldError: CodeError() {
            object InvalidActivationCodeError : TextFieldError()
        }
        sealed class DialogError: CodeError() {
            object InvalidEmailError: DialogError()
            object AccountAlreadyExistsError: DialogError()
            object BlackListedError: DialogError()
            object EmailDomainBlockedError: DialogError()
            object TeamMembersLimitError: DialogError()
            object CreationRestrictedError: DialogError()
            data class GenericError(val coreFailure: NetworkFailure): DialogError()
        }
    }
}
