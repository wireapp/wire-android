package com.wire.android.ui.authentication.login.sso

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreFailure

data class LoginSSOState(
    val ssoCode: TextFieldValue = TextFieldValue(""),
    val loading: Boolean = false,
    val loginEnabled: Boolean = false,
    val loginSSOError: LoginSSOError = LoginSSOError.None,
)

sealed class LoginSSOError {
    object None: LoginSSOError()
    sealed class TextFieldError: LoginSSOError() {
        object InvalidCodeError: TextFieldError()
    }
    sealed class DialogError: LoginSSOError() {
        data class GenericError(val coreFailure: CoreFailure) : DialogError()
        data class ResultError @OptIn(ExperimentalMaterial3Api::class) constructor(val result: SSOFailureCodes) : DialogError()
    }
}
