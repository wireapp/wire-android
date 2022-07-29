package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.auth.AuthSession

sealed class LoginError {
    object None : LoginError()
    sealed class TextFieldError : LoginError() {
        object InvalidValue : TextFieldError()
    }

    sealed class DialogError : LoginError() {
        data class GenericError(val coreFailure: CoreFailure) : DialogError()
        object InvalidCredentialsError : DialogError()
        object InvalidSSOCookie : DialogError()
        object InvalidCodeError : DialogError()
        object UserAlreadyExists : DialogError()
        object PasswordNeededToRegisterClient : DialogError()
        data class InvalidSessionError(val session: AuthSession) : DialogError()

        data class SSOResultError constructor(val result: SSOFailureCodes) :
            DialogError()
    }

    object TooManyDevicesError : LoginError()
}
