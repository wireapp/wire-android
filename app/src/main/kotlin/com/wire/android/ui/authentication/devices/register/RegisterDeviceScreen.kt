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

package com.wire.android.ui.authentication.devices.register

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@WireDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun RegisterDeviceScreen(
    navigator: Navigator,
    viewModel: RegisterDeviceViewModel = hiltViewModel(),
    clearSessionViewModel: ClearSessionViewModel = hiltViewModel(),
) {
    clearAutofillTree()
    when (val flowState = viewModel.state.flowState) {
        is RegisterDeviceFlowState.Success -> {
            navigator.navigate(
                NavigationCommand(
                    destination = if (flowState.isE2EIRequired) E2EIEnrollmentScreenDestination
                    else if (flowState.initialSyncCompleted) HomeScreenDestination
                    else InitialSyncScreenDestination,
                    backStackMode = BackStackMode.CLEAR_WHOLE
                )
            )
        }

        is RegisterDeviceFlowState.TooManyDevices -> navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination))
        else ->
            AnimatedContent(
                targetState = viewModel.secondFactorVerificationCodeState.isCodeInputNecessary,
                transitionSpec = {
                    TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
                },
                modifier = Modifier.fillMaxSize()
            ) { isCodeInputNecessary ->
                if (isCodeInputNecessary) {
                    RegisterDeviceVerificationCodeScreen(viewModel)
                } else {
                    RegisterDeviceContent(
                        state = viewModel.state,
                        passwordTextState = viewModel.passwordTextState,
                        clearSessionState = clearSessionViewModel.state,
                        onContinuePressed = viewModel::onContinue,
                        onErrorDismiss = viewModel::onErrorDismiss,
                        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
                        onCancelLoginClicked = {
                            clearSessionViewModel.onCancelLoginClicked(
                                NavigationSwitchAccountActions(navigator::navigate)
                            )
                        },
                        onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked
                    )
                }
            }
    }
}

@Composable
private fun RegisterDeviceContent(
    state: RegisterDeviceState,
    passwordTextState: TextFieldState,
    clearSessionState: ClearSessionState,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    onProceedLoginClicked: () -> Unit,
    modifier: Modifier = Modifier,
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

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.register_device_title),
                navigationIconType = NavigationIconType.Close(),
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
            PasswordTextField(state = state, passwordTextState = passwordTextState)
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_add_device),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.flowState is RegisterDeviceFlowState.Loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .testTag("registerButton")
            )
        }
    }
    if (state.flowState is RegisterDeviceFlowState.Error.GenericError) {
        CoreFailureErrorDialog(state.flowState.coreFailure, onErrorDismiss)
    }
}

@Composable
private fun PasswordTextField(
    state: RegisterDeviceState,
    passwordTextState: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = passwordTextState,
        state = when (state.flowState) {
            is RegisterDeviceFlowState.Error.InvalidCredentialsError ->
                WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))

            else -> WireTextFieldState.Default
        },
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        modifier = modifier
            .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
            .testTag("password field"),
        autoFill = true
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewRegisterDeviceScreen() = WireTheme {
    RegisterDeviceContent(RegisterDeviceState(), TextFieldState(), ClearSessionState(), {}, {}, {}, {}, {})
}
