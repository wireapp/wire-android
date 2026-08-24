/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.login

sealed class LoginState<out FailureT, out UserT, out SsoFailureT> {
    data object Default : LoginState<Nothing, Nothing, Nothing>()
    data object Loading : LoginState<Nothing, Nothing, Nothing>()
    data object Canceled : LoginState<Nothing, Nothing, Nothing>()
    data class Success<out UserT>(
        val initialSyncCompleted: Boolean,
        val isE2EIRequired: Boolean,
        val userId: UserT,
    ) : LoginState<Nothing, UserT, Nothing>()

    sealed class Error<out FailureT, out UserT, out SsoFailureT> : LoginState<FailureT, UserT, SsoFailureT>() {
        sealed class TextFieldError : Error<Nothing, Nothing, Nothing>() {
            data object InvalidValue : TextFieldError()
        }

        sealed class DialogError<out FailureT, out SsoFailureT> : Error<FailureT, Nothing, SsoFailureT>() {
            data class GenericError<out FailureT>(val coreFailure: FailureT) : DialogError<FailureT, Nothing>()
            data object InvalidCredentialsError : DialogError<Nothing, Nothing>()
            data object ProxyError : DialogError<Nothing, Nothing>()
            data object InvalidSSOCookie : DialogError<Nothing, Nothing>()
            data object InvalidSSOCodeError : DialogError<Nothing, Nothing>()
            data object UserAlreadyExists : DialogError<Nothing, Nothing>()
            data object PasswordNeededToRegisterClient : DialogError<Nothing, Nothing>()
            data object Request2FAWithHandle : DialogError<Nothing, Nothing>()
            data class SSOResultError<out SsoFailureT>(val result: SsoFailureT) : DialogError<Nothing, SsoFailureT>()
            data object ServerVersionNotSupported : DialogError<Nothing, Nothing>()
            data object ClientUpdateRequired : DialogError<Nothing, Nothing>()
            data object AccountSuspended : DialogError<Nothing, Nothing>()
            data object AccountPendingActivation : DialogError<Nothing, Nothing>()
        }

        data class TooManyDevicesError<out UserT>(val userId: UserT) : Error<Nothing, UserT, Nothing>()
    }
}
