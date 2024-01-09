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

package com.wire.android.ui.authentication.create.details

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.kalium.logic.NetworkFailure

data class CreateAccountDetailsViewState(
    val type: CreateAccountFlowType,
    val firstName: TextFieldValue = TextFieldValue(""),
    val lastName: TextFieldValue = TextFieldValue(""),
    val password: TextFieldValue = TextFieldValue(""),
    val confirmPassword: TextFieldValue = TextFieldValue(""),
    val teamName: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: DetailsError = DetailsError.None
) {
    fun fieldsNotEmpty(): Boolean =
        firstName.text.isNotBlank() && lastName.text.isNotBlank() && password.text.isNotBlank() && confirmPassword.text.isNotBlank()
                && (type == CreateAccountFlowType.CreatePersonalAccount || teamName.text.isNotBlank())

    sealed class DetailsError {
        object None : DetailsError()
        sealed class TextFieldError : DetailsError() {
            object InvalidPasswordError : TextFieldError()
            object PasswordsNotMatchingError : TextFieldError()
        }

        sealed class DialogError : DetailsError() {
            data class GenericError(val coreFailure: NetworkFailure) : DialogError()
        }
    }
}
