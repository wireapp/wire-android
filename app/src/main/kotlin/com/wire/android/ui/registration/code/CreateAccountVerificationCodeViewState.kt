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

package com.wire.android.ui.registration.code

import com.wire.kalium.common.error.CoreFailure

data class CreateAccountVerificationCodeViewState(
    val codeLength: Int = DEFAULT_VERIFICATION_CODE_LENGTH,
    val email: String = "",
    val loading: Boolean = false,
    val result: CreateAccountCodeResult = CreateAccountCodeResult.None,
) {

    companion object {
        const val DEFAULT_VERIFICATION_CODE_LENGTH = 6
    }
}

sealed interface CreateAccountCodeResult {
    data object None : CreateAccountCodeResult
    data object Success : CreateAccountCodeResult
    sealed class Error : CreateAccountCodeResult {
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
