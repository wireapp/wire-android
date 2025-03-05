/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.newauthentication.login

import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.configuration.server.ServerConfig


sealed class NewLoginFlowState {
    data object Default : NewLoginFlowState()
    data object Loading : NewLoginFlowState()
    data class CustomConfigDialog(val serverLinks: ServerConfig.Links) : NewLoginFlowState()
    sealed class Error : NewLoginFlowState() {
        sealed class TextFieldError : Error() {
            data object InvalidValue : TextFieldError()
        }

        // subset of LoginState.Error.DialogError
        sealed class DialogError : Error() {
            data object ServerVersionNotSupported : DialogError()
            data object ClientUpdateRequired : DialogError()
            data class SSOResultFailure(val result: SSOFailureCodes) : DialogError()
            data object InvalidSSOCode : DialogError()
            data object InvalidSSOCookie : DialogError()
            data object UserAlreadyExists : DialogError()
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }
    }
}

fun NewLoginFlowState.Error.DialogError.toLoginStateDialogError(): LoginState.Error.DialogError = when (this) {
    is NewLoginFlowState.Error.DialogError.ServerVersionNotSupported -> LoginState.Error.DialogError.ServerVersionNotSupported
    is NewLoginFlowState.Error.DialogError.ClientUpdateRequired -> LoginState.Error.DialogError.ClientUpdateRequired
    is NewLoginFlowState.Error.DialogError.SSOResultFailure -> LoginState.Error.DialogError.SSOResultError(this.result)
    is NewLoginFlowState.Error.DialogError.InvalidSSOCode -> LoginState.Error.DialogError.InvalidSSOCodeError
    is NewLoginFlowState.Error.DialogError.InvalidSSOCookie -> LoginState.Error.DialogError.InvalidSSOCookie
    is NewLoginFlowState.Error.DialogError.UserAlreadyExists -> LoginState.Error.DialogError.UserAlreadyExists
    is NewLoginFlowState.Error.DialogError.GenericError -> LoginState.Error.DialogError.GenericError(coreFailure)
}
