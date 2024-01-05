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

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.CoreFailure

data class CreateAccountCodeViewState(
    val type: CreateAccountFlowType,
    val code: CodeFieldValue = CodeFieldValue(TextFieldValue(""), false),
    val email: String = "",
    val loading: Boolean = false,
    val error: CodeError = CodeError.None
) {
    sealed class CodeError {
        object None : CodeError()
        sealed class TextFieldError : CodeError() {
            object InvalidActivationCodeError : TextFieldError()
        }

        sealed class DialogError : CodeError() {
            object InvalidEmailError : DialogError()
            object AccountAlreadyExistsError : DialogError()
            object BlackListedError : DialogError()
            object EmailDomainBlockedError : DialogError()
            object TeamMembersLimitError : DialogError()
            object CreationRestrictedError : DialogError()
            object UserAlreadyExists: DialogError()
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }

        object TooManyDevicesError : CodeError()
    }
}
