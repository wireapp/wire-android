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
 *
 */

package com.wire.android.ui.authentication.devices.register

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RegisterDeviceScreen() {
    val viewModel: RegisterDeviceViewModel = hiltViewModel()
    val clearSessionViewModel: ClearSessionViewModel = hiltViewModel()
    val clearSessionState: ClearSessionState = clearSessionViewModel.state
    clearAutofillTree()
    RegisterDeviceContent(
        state = viewModel.state,
        clearSessionState = clearSessionState,
        onPasswordChange = viewModel::onPasswordChange,
        onContinuePressed = viewModel::onContinue,
        onErrorDismiss = viewModel::onErrorDismiss,
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onCancelLoginClicked = clearSessionViewModel::onCancelLoginClicked,
        onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked
    )
}

@Composable
private fun RegisterDeviceContent(
    state: RegisterDeviceState,
    clearSessionState: ClearSessionState,
    onPasswordChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    onProceedLoginClicked: () -> Unit
) {
    BackHandler {
        onBackButtonClicked()
    }
    val cancelLoginDialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(
        dialogState = cancelLoginDialogState,
        onActionButtonClicked = {
            onCancelLoginClicked()
        },
        onProceedButtonClicked = {
            onProceedLoginClicked()
        }
    )
    if (clearSessionState.showCancelLoginDialog) {
        cancelLoginDialogState.show(
            cancelLoginDialogState.savedState ?: CancelLoginDialogState
        )
    } else {
        cancelLoginDialogState.dismiss()
    }

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.register_device_title),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = onBackButtonClicked
            )
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(internalPadding)
        ) {
            Text(
                text = stringResource(id = R.string.register_device_text),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
                    .testTag("registerText")
            )
            PasswordTextField(state = state, onPasswordChange = onPasswordChange)
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_add_device),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .testTag("registerButton")
            )
        }
    }
    if (state.error is RegisterDeviceError.GenericError) {
        CoreFailureErrorDialog(state.error.coreFailure, onErrorDismiss)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordTextField(state: RegisterDeviceState, onPasswordChange: (TextFieldValue) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        state = when (state.error) {
            is RegisterDeviceError.InvalidCredentialsError ->
                WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))
            else -> WireTextFieldState.Default
        },
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = Modifier
            .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
            .testTag("password field"),
        autofill = true
    )
}

@Composable
@Preview
fun PreviewRegisterDeviceScreen() {
    RegisterDeviceContent(RegisterDeviceState(), ClearSessionState(), {}, {}, {}, {}, {}, {})
}
