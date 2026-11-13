/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun MultiFactorAuthenticationCodeInput(
    code: TextFieldState,
    isBusy: Boolean,
    isInvalid: Boolean = false,
) {
    val label = stringResource(R.string.multi_factor_authentication_code_label)
    Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing8x)) {
        Text(
            text = label,
            style = MaterialTheme.wireTypography.label01,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        CodeTextField(
            textState = code,
            codeLength = AUTHENTICATION_CODE_LENGTH,
            state = when {
                isBusy -> WireTextFieldState.Disabled
                isInvalid -> WireTextFieldState.Error()
                else -> WireTextFieldState.Default
            },
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
