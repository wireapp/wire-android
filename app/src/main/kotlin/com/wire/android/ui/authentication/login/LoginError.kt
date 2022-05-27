package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreFailure

sealed class LoginError {
    object None : LoginError()
    sealed class TextFieldError : LoginError() {
        object InvalidValue : TextFieldError()
    }

    sealed class DialogError : LoginError() {
        data class GenericError(val coreFailure: CoreFailure) : LoginError.DialogError()
        object InvalidCredentialsError : DialogError()
        object InvalidSSOCookie : DialogError()
        object InvalidCodeError: DialogError()
        object UserAlreadyExists : DialogError()
        data class SSOResultError constructor(val result: SSOFailureCodes) :
            LoginError.DialogError()
    }

    object TooManyDevicesError : LoginError()

}
