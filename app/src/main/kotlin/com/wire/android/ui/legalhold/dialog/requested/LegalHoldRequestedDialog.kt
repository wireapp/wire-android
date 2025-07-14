/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.legalhold.dialog.common.LearnMoreAboutLegalHoldButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.extension.formatAsFingerPrint
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

@Composable
fun LegalHoldRequestedDialog(
    state: LegalHoldRequestedState.Visible,
    passwordTextState: TextFieldState,
    notNowClicked: () -> Unit,
    acceptClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyboardController: SoftwareKeyboardController? = null
    WireDialog(
        modifier = modifier,
        title = stringResource(R.string.legal_hold_requested_dialog_title),
        properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false),
        onDismiss = { keyboardController?.hide() },
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = notNowClicked,
            text = stringResource(id = R.string.legal_hold_requested_dialog_not_now_button),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                keyboardController?.hide()
                acceptClicked()
            },
            text = stringResource(R.string.legal_hold_requested_dialog_accept_button),
            type = WireDialogButtonType.Primary,
            loading = state.loading,
            state = if (state.acceptEnabled) WireButtonState.Error else WireButtonState.Disabled
        ),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.dialogTextsSpacing),
                modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.dialogTextsSpacing)
            ) {
                Text(
                    text = stringResource(id = R.string.legal_hold_requested_dialog_description_device),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = state.legalHoldDeviceFingerprint.formatAsFingerPrint(),
                    style = MaterialTheme.wireTypography.body01,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(id = R.string.legal_hold_requested_dialog_description_includes),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier.fillMaxWidth()
                )
                LearnMoreAboutLegalHoldButton()
                if (state.requiresPassword) {
                    Text(
                        text = stringResource(id = R.string.legal_hold_requested_dialog_enter_password),
                        style = MaterialTheme.wireTypography.body01,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
                    // to the dialog's content and use keyboard controller from there
                    keyboardController = LocalSoftwareKeyboardController.current
                    val focusRequester = remember { FocusRequester() }
                    WirePasswordTextField(
                        textState = passwordTextState,
                        state = when {
                            state.error is LegalHoldRequestedError.InvalidCredentialsError ->
                                WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))

                            state.loading -> WireTextFieldState.Disabled
                            else -> WireTextFieldState.Default
                        },
                        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                        onKeyboardAction = { keyboardController?.hide() },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                            .testTag("remove device password field"),
                        autoFill = true
                    )
                }
            }
        }
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldRequestedDialogWithPassword() {
    WireTheme {
        LegalHoldRequestedDialog(
            state = LegalHoldRequestedState.Visible(
                legalHoldDeviceFingerprint = "0123456789ABCDEF",
                requiresPassword = true,
                userId = UserId("", ""),
            ),
            passwordTextState = TextFieldState(),
            notNowClicked = {},
            acceptClicked = {},
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewLegalHoldRequestedDialogWithoutPassword() {
    WireTheme {
        LegalHoldRequestedDialog(
            state = LegalHoldRequestedState.Visible(
                legalHoldDeviceFingerprint = "0123456789ABCDEF",
                requiresPassword = false,
                userId = UserId("", ""),
            ),
            passwordTextState = TextFieldState(),
            notNowClicked = {},
            acceptClicked = {},
        )
    }
}
