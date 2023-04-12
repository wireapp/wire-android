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
 */

package com.wire.android.ui.authentication.verificationcode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.R
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun VerificationCode(
    codeLength: Int,
    currentCode: TextFieldValue,
    isLoading: Boolean,
    isCurrentCodeInvalid: Boolean,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCode: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {

        val state = if (isCurrentCodeInvalid) WireTextFieldState.Error(
            stringResource(id = R.string.second_factor_code_error)
        )
        else WireTextFieldState.Default

        CodeTextField(
            codeLength = codeLength,
            value = currentCode,
            state = state,
            onValueChange = onCodeChange,
            modifier = Modifier.focusRequester(focusRequester)
        )

        AnimatedVisibility(visible = isLoading) {
            WireCircularProgressIndicator(
                progressColor = MaterialTheme.wireColorScheme.primary,
                size = MaterialTheme.wireDimensions.spacing24x,
                modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing16x)
            )
        }

        ResendCodeText(
            onResendCodePressed = onResendCode,
            clickEnabled = !isLoading
        )
    }
}
