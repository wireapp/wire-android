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
package com.wire.android.ui.legalhold.dialog.requested

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class LegalHoldRequestedState(
    val done: Boolean = false,
    val legalHoldDeviceFingerprint: String = "",
    val password: TextFieldValue = TextFieldValue(""),
    val requiresPassword: Boolean = false,
    val loading: Boolean = false,
    val acceptEnabled: Boolean = false,
    val error: LegalHoldRequestedError = LegalHoldRequestedError.None,
)

sealed class LegalHoldRequestedError {
    data object None : LegalHoldRequestedError()
    data object InvalidCredentialsError : LegalHoldRequestedError()
    data class GenericError(val coreFailure: CoreFailure) : LegalHoldRequestedError()
}
