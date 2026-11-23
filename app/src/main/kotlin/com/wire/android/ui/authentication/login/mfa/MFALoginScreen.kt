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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.mfa.MFACodeInput
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun MFALoginScreen(
    code: TextFieldState,
    isBusy: Boolean,
    isCodeInvalid: Boolean,
    canSubmit: Boolean,
    onCodeComplete: () -> Unit,
    onBack: () -> Unit,
    onUseAnotherMethod: () -> Unit,
    authenticatorName: String? = null,
) {
    BackHandler(onBack = onBack)
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    NewAuthTitle(title = stringResource(R.string.mfa_login_code_title))
                    NewAuthSubtitle(
                        title = if (authenticatorName != null) {
                            stringResource(R.string.mfa_login_named_authenticator_description, authenticatorName)
                        } else {
                            stringResource(R.string.mfa_login_code_description)
                        }
                    )
                },
                canNavigateBack = true,
                onNavigateBack = onBack,
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("mfaLoginCodeScreen"),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            MFACodeInput(
                code = code,
                isBusy = isBusy,
                isInvalid = isCodeInvalid,
                canSubmit = canSubmit,
                onCodeComplete = onCodeComplete,
            )
            if (isCodeInvalid) {
                Text(
                    text = stringResource(R.string.mfa_invalid_code),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.error,
                    modifier = Modifier.testTag("mfaLoginInvalidCode"),
                )
            }
            TextButton(
                enabled = canSubmit && !isBusy,
                onClick = onUseAnotherMethod,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.mfa_login_use_another_method))
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFALoginCodeScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            MFALoginScreen(
                code = remember { TextFieldState() },
                isBusy = false,
                isCodeInvalid = false,
                canSubmit = true,
                onCodeComplete = {},
                onBack = {},
                onUseAnotherMethod = {},
            )
        }
    }
}
