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

import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.CoreFailure

data class CreateAccountEmailViewState(
    val type: CreateAccountFlowType,
    val termsDialogVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: EmailError = EmailError.None,
    val showClientUpdateDialog: Boolean = false,
    val showServerVersionNotSupportedDialog: Boolean = false
) {
    sealed class EmailError {
        data object None : EmailError()
        sealed class TextFieldError : EmailError() {
            data object InvalidEmailError : TextFieldError()
            data object BlacklistedEmailError : TextFieldError()
            data object AlreadyInUseError : TextFieldError()
            data object DomainBlockedError : TextFieldError()
        }

        sealed class DialogError : EmailError() {
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }
    }
}
