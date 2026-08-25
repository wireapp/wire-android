/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

sealed interface NewLoginFlowState<out LinksT, out FailureT, out SsoFailureT> {
    data object Default : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object Loading : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object MissingBackendConfig : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object LoadingBackendConfig : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object BackendConfigError : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object BackendConfigSuccess : NewLoginFlowState<Nothing, Nothing, Nothing>
    data object SsoIdentityChanged : NewLoginFlowState<Nothing, Nothing, Nothing>
    data class CustomConfigDialog<LinksT>(val serverLinks: LinksT) : NewLoginFlowState<LinksT, Nothing, Nothing>

    sealed interface Error<out FailureT, out SsoFailureT> : NewLoginFlowState<Nothing, FailureT, SsoFailureT> {
        sealed interface TextFieldError : Error<Nothing, Nothing> {
            data object InvalidValue : TextFieldError
        }

        sealed interface DialogError<out FailureT, out SsoFailureT> : Error<FailureT, SsoFailureT> {
            data object ServerVersionNotSupported : DialogError<Nothing, Nothing>
            data object ClientUpdateRequired : DialogError<Nothing, Nothing>
            data class SSOResultFailure<SsoFailureT>(val result: SsoFailureT) : DialogError<Nothing, SsoFailureT>
            data object InvalidSSOCode : DialogError<Nothing, Nothing>
            data object InvalidSSOCookie : DialogError<Nothing, Nothing>
            data object UserAlreadyExists : DialogError<Nothing, Nothing>
            data class GenericError<FailureT>(val failure: FailureT) : DialogError<FailureT, Nothing>
        }
    }
}
