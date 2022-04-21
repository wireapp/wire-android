package com.wire.android.ui.authentication.devices.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RegisterDeviceScreen() {
    val viewModel: RegisterDeviceViewModel = hiltViewModel()
    RegisterDeviceContent(
        state = viewModel.state,
        onPasswordChange = viewModel::onPasswordChange,
        onContinuePressed = viewModel::onContinue,
        onErrorDismiss = viewModel::onErrorDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun RegisterDeviceContent(
    state: RegisterDeviceState,
    onPasswordChange: (TextFieldValue) -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.register_device_title),
                navigationIconType = null
            )
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(id = R.string.register_device_text),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    ).testTag("register text")
            )
            PasswordTextField(state = state, onPasswordChange = onPasswordChange)
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_register),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x).testTag("register button")
            )
        }
    }
    if (state.error is RegisterDeviceError.GenericError)
        CoreFailureErrorDialog(state.error.coreFailure, onErrorDismiss)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordTextField(state: RegisterDeviceState, onPasswordChange: (TextFieldValue) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            state = when(state.error) {
                is RegisterDeviceError.InvalidCredentialsError ->
                    WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))
                else -> WireTextFieldState.Default
            },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).testTag("password field")
        )
}

@Composable
@Preview
private fun RegisterDeviceScreenPreview() {
    RegisterDeviceContent(RegisterDeviceState(), {}, {}, {})
}
