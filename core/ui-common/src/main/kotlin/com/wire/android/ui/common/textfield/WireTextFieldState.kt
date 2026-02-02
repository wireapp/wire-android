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
package com.wire.android.ui.common.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import com.wire.android.ui.common.R
import kotlinx.coroutines.flow.Flow

sealed class WireTextFieldState {
    data object Default : WireTextFieldState()
    data class Error(
        val errorText: String? = null,
        val withStartPadding: Boolean = false
    ) : WireTextFieldState()

    data object Success : WireTextFieldState()
    data object Disabled : WireTextFieldState()
    data object ReadOnly : WireTextFieldState()

    fun icon(): Int? = when (this) {
        is Error -> R.drawable.ic_error_outline
        is Success -> R.drawable.ic_check
        else -> null
    }
}

fun TextFieldState.textAsFlow(): Flow<CharSequence> = snapshotFlow { text }
