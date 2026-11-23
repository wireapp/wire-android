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

package com.wire.android.ui.common.mfa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.mfa.MFAConstraints.AUTHENTICATION_CODE_LENGTH

@Composable
internal fun MFACodeInput(
    code: TextFieldState,
    isBusy: Boolean,
    onCodeComplete: () -> Unit,
    isInvalid: Boolean = false,
    canSubmit: Boolean = true,
) {
    val submissionEnabled = rememberUpdatedState(!isBusy && canSubmit)
    val currentOnCodeComplete = rememberUpdatedState(onCodeComplete)
    LaunchedEffect(code) {
        observeMFACodeCompletion(
            code = code,
            canSubmit = { submissionEnabled.value },
            onCodeComplete = { currentOnCodeComplete.value() },
        )
    }
    val label = stringResource(R.string.mfa_code_label)
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
        if (isBusy) {
            WireCircularProgressIndicator(
                progressColor = MaterialTheme.wireColorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/** Submits each completed code entry once, allowing the same code to be retried after clearing the field. */
internal suspend fun observeMFACodeCompletion(
    code: TextFieldState,
    canSubmit: () -> Boolean,
    onCodeComplete: () -> Unit,
) {
    var submittedCode: String? = null
    snapshotFlow { code.text.toString() to canSubmit() }.collect { (value, enabled) ->
        val isComplete = value.length == AUTHENTICATION_CODE_LENGTH && value.all { it in '0'..'9' }
        if (!isComplete) {
            submittedCode = null
        } else if (enabled && value != submittedCode) {
            submittedCode = value
            onCodeComplete()
        }
    }
}
