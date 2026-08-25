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

package com.wire.android.ui.authentication.create.email

data class CreateAccountEmailViewState<FlowT, FailureT>(
    val type: FlowT,
    val termsDialogVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: EmailError<FailureT> = EmailError.None,
    val showClientUpdateDialog: Boolean = false,
    val showServerVersionNotSupportedDialog: Boolean = false,
    val success: Boolean = false,
) {
    sealed interface EmailError<out FailureT> {
        data object None : EmailError<Nothing>
        sealed interface TextFieldError : EmailError<Nothing> {
            data object InvalidEmailError : TextFieldError
            data object BlacklistedEmailError : TextFieldError
            data object AlreadyInUseError : TextFieldError
            data object DomainBlockedError : TextFieldError
        }

        sealed interface DialogError<out FailureT> : EmailError<FailureT> {
            data class GenericError<FailureT>(val coreFailure: FailureT) : DialogError<FailureT>
        }
    }
}
