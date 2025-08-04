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

package com.wire.android.ui.authentication.create.code

import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.common.error.CoreFailure

data class CreateAccountCodeViewState(
    val type: CreateAccountFlowType,
    val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH,
    val email: String = "",
    val loading: Boolean = false,
    val result: Result = Result.None,
    val remainingTimerText: String? = null,
) {
    sealed interface Result {
        data object None : Result
        data object Success : Result
        sealed class Error : Result {
            sealed class TextFieldError : Error() {
                data object InvalidActivationCodeError : TextFieldError()
            }

            sealed class DialogError : Error() {
                data object InvalidEmailError : DialogError()
                data object AccountAlreadyExistsError : DialogError()
                data object BlackListedError : DialogError()
                data object EmailDomainBlockedError : DialogError()
                data object TeamMembersLimitError : DialogError()
                data object CreationRestrictedError : DialogError()
                data object UserAlreadyExistsError : DialogError()
                data class GenericError(val coreFailure: CoreFailure) : DialogError()
            }
            data object TooManyDevicesError : Error()
        }
    }
    companion object {
        const val DEFAULT_VERIFICATION_CODE_LENGTH = 6
    }
}
