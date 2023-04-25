/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.account.email.updateEmail

import androidx.compose.ui.text.input.TextFieldValue

data class ChangeEmailState(
    val email: TextFieldValue = TextFieldValue(""),
    val error: EmailError = EmailError.None,
    val isEmailTextEditEnabled: Boolean = true,
    val animatedEmailError: Boolean = false,
    val saveEnabled: Boolean = false,
) {
    sealed interface EmailError {
        object None : EmailError
        sealed interface TextFieldError : EmailError {
            object AlreadyInUse : TextFieldError
            object InvalidEmail : TextFieldError
            object Generic : TextFieldError
        }
    }
}
