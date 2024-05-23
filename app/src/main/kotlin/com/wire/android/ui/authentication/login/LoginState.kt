/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreFailure

sealed class LoginState {
    data object Default : LoginState()
    data object Loading : LoginState()
    data class Success(val initialSyncCompleted: Boolean, val isE2EIRequired: Boolean) : LoginState()
    sealed class Error : LoginState() {
        sealed class TextFieldError : Error() {
            data object InvalidValue : TextFieldError()
        }
        sealed class DialogError : Error() {
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
            data object InvalidCredentialsError : DialogError()
            data object ProxyError : DialogError()
            data object InvalidSSOCookie : DialogError()
            data object InvalidSSOCodeError : DialogError()
            data object UserAlreadyExists : DialogError()
            data object PasswordNeededToRegisterClient : DialogError()
            data class SSOResultError(val result: SSOFailureCodes) :
                DialogError()
            data object ServerVersionNotSupported: DialogError()
            data object ClientUpdateRequired: DialogError()
        }
        data object TooManyDevicesError : Error()
    }
}
