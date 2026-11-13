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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun MultiFactorAuthenticationTurnOffScreen(
    state: MultiFactorAuthenticationTurnOffState,
    password: TextFieldState,
    code: TextFieldState,
    onBack: () -> Unit,
    onTurnOff: () -> Unit,
) {
    MultiFactorAuthenticationCredentialsScreen(
        title = stringResource(R.string.multi_factor_authentication_turn_off_title),
        heading = stringResource(R.string.multi_factor_authentication_turn_off_heading),
        description = stringResource(R.string.multi_factor_authentication_turn_off_description),
        confirmLabel = stringResource(R.string.multi_factor_authentication_turn_off),
        password = password,
        code = code,
        isBusy = state.isBusy,
        actionAvailable = !state.isCompleted,
        errorText = state.error?.let {
            stringResource(
                when (it) {
                    MultiFactorAuthenticationTurnOffError.INVALID_CODE -> R.string.multi_factor_authentication_invalid_code
                    MultiFactorAuthenticationTurnOffError.GENERIC -> R.string.multi_factor_authentication_setup_error
                }
            )
        },
        onBack = onBack,
        onConfirm = onTurnOff,
    )
}

@Composable
internal fun MultiFactorAuthenticationCredentialsScreen(
    title: String,
    heading: String,
    description: String,
    confirmLabel: String,
    password: TextFieldState,
    code: TextFieldState,
    isBusy: Boolean,
    actionAvailable: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(onBack = onBack)
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = title,
                onNavigationPressed = onBack,
                elevation = dimensions().spacing0x,
            )
        },
        bottomBar = {
            val enabled = !isBusy && actionAvailable && canSubmitMultiFactorAuthenticationCredentials(password.text, code.text)
            WirePrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                state = if (enabled) WireButtonState.Error else WireButtonState.Disabled,
                loading = isBusy,
                modifier = Modifier.padding(dimensions().spacing16x),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
        ) {
            Text(
                text = heading,
                style = MaterialTheme.wireTypography.title01,
            )
            Text(
                text = description,
                style = MaterialTheme.wireTypography.body01,
            )
            val fieldState = if (isBusy) WireTextFieldState.Disabled else WireTextFieldState.Default
            WirePasswordTextField(textState = password, state = fieldState)
            WireTextField(
                textState = code,
                labelText = stringResource(R.string.multi_factor_authentication_code_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                inputTransformation = InputTransformation {
                    if (length > AUTHENTICATION_CODE_LENGTH || asCharSequence().any { it !in '0'..'9' }) revertAllChanges()
                },
                state = fieldState,
            )
            Text(
                text = stringResource(R.string.multi_factor_authentication_turn_off_demo),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
            errorText?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.error,
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewMultiFactorAuthenticationTurnOff() = WireTheme {
    MultiFactorAuthenticationTurnOffScreen(
        state = MultiFactorAuthenticationTurnOffState(),
        password = remember { TextFieldState() },
        code = remember { TextFieldState() },
        onBack = {},
        onTurnOff = {},
    )
}
