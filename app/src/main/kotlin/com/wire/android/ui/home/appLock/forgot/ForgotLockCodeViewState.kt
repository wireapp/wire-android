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
package com.wire.android.ui.home.appLock.forgot

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class ForgotLockCodeViewState(
    val completed: Boolean = false,
    val error: CoreFailure? = null,
    val dialogState: ForgotLockCodeDialogState = ForgotLockCodeDialogState.Hidden,
)

sealed class ForgotLockCodeDialogState {
    data object Hidden : ForgotLockCodeDialogState()
    data class Visible(
        val username: String,
        val password: TextFieldValue = TextFieldValue(""),
        val passwordNotRequired: Boolean = false,
        val passwordValid: Boolean = true,
        val resetDeviceEnabled: Boolean = false,
        val loading: Boolean = false,
    ) : ForgotLockCodeDialogState()
}
