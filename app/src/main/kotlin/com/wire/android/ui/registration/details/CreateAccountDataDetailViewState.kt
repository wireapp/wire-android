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

package com.wire.android.ui.registration.details

import com.wire.kalium.common.error.CoreFailure

data class CreateAccountDataDetailViewState(
    val privacyPolicyAccepted: Boolean = false,
    val termsDialogVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: DetailsError = DetailsError.None,
    val success: Boolean = false,
) {
    sealed class DetailsError {
        data object None : DetailsError()
        sealed class PasswordError : DetailsError() {
            data object InvalidPasswordError : PasswordError()
            data object PasswordsNotMatchingError : PasswordError()
        }

        sealed class EmailFieldError : DetailsError() {
            data object InvalidEmailError : EmailFieldError()
            data object BlacklistedEmailError : EmailFieldError()
            data object AlreadyInUseError : EmailFieldError()
            data object DomainBlockedError : EmailFieldError()
        }

        sealed class DialogError : DetailsError() {
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }

        fun isPasswordError() = this is PasswordError
        fun isEmailError() = this is EmailFieldError
    }
}
