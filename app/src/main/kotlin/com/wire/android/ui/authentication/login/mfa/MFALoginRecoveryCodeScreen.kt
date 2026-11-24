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

package com.wire.android.ui.authentication.login.mfa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.R
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.login.email.LoginButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun MFALoginRecoveryCodeScreen(
    code: TextFieldState,
    isBusy: Boolean,
    isCodeInvalid: Boolean,
    canSubmit: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { if (!isBusy) onBack() }
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    NewAuthTitle(title = stringResource(R.string.mfa_login_recovery_code_title))
                    NewAuthSubtitle(title = stringResource(R.string.mfa_login_recovery_code_description))
                },
                canNavigateBack = !isBusy,
                onNavigateBack = onBack,
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("mfaLoginRecoveryCodeScreen"),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            WireTextField(
                textState = code,
                labelText = stringResource(R.string.mfa_login_method_recovery_code),
                state = when {
                    isBusy -> WireTextFieldState.Disabled
                    isCodeInvalid -> WireTextFieldState.Error()
                    else -> WireTextFieldState.Default
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
                onKeyboardAction = { if (canSubmit && !isBusy) onConfirm() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "mfaLoginRecoveryCodeInput",
            )
            if (isCodeInvalid) {
                Text(
                    text = stringResource(R.string.mfa_login_invalid_recovery_code),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.error,
                    modifier = Modifier.testTag("mfaLoginInvalidRecoveryCode"),
                )
            }
            Text(
                text = stringResource(R.string.mfa_login_recovery_code_help),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
            LoginButton(
                loading = isBusy,
                enabled = canSubmit,
                text = stringResource(R.string.mfa_login_recovery_submit),
                loadingText = stringResource(R.string.mfa_login_recovery_submit),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                enabled = !isBusy,
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.mfa_login_use_another_method))
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFALoginRecoveryCodeScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            val code = remember { TextFieldState() }
            MFALoginRecoveryCodeScreen(
                code = code,
                isBusy = false,
                isCodeInvalid = false,
                canSubmit = code.text.isNotBlank(),
                onConfirm = {},
                onBack = {},
            )
        }
    }
}
