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
 *
 *
 */

package com.wire.android.ui.authentication.create.username

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class CreateAccountUsernameViewState(
    val username: TextFieldValue = TextFieldValue(""),
    val animateUsernameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: UsernameError = UsernameError.None
) {
    sealed class UsernameError {
        object None : UsernameError()
        sealed class TextFieldError : UsernameError() {
            object UsernameTakenError : TextFieldError()
            object UsernameInvalidError : TextFieldError()
        }

        sealed class DialogError : UsernameError() {
            data class GenericError(val coreFailure: CoreFailure) : DialogError()
        }
    }
}
