/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

data class RegisterDeviceText(
    val title: String,
    val message: String,
    val continueLabel: String,
    val invalidPasswordMessage: String,
)

/** Feature-owned register-device presentation; session cancellation and shared dialogs are slots. */
@Composable
fun RegisterDeviceScreen(
    viewModel: RegisterDeviceViewModel,
    text: RegisterDeviceText,
    cancelDialog: @Composable () -> Unit,
    failureDialog: @Composable (AuthenticationFailure, onDismiss: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onSuccess: (RegisterDeviceFlowState.Success) -> Unit,
    onTooManyDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flowState = viewModel.state.flowState
    LaunchedEffect(flowState) {
        when (flowState) {
            is RegisterDeviceFlowState.Success -> onSuccess(flowState)
            RegisterDeviceFlowState.TooManyDevices -> onTooManyDevices()
            else -> Unit
        }
    }
    when (flowState) {
        is RegisterDeviceFlowState.Success, RegisterDeviceFlowState.TooManyDevices -> Unit
        else -> AnimatedContent(
            targetState = viewModel.secondFactorVerificationCodeState.isCodeInputNecessary,
            transitionSpec = {
                TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
            },
            modifier = modifier.fillMaxSize(),
        ) { needsCode ->
            if (needsCode) RegisterDeviceVerificationCodeScreen(viewModel)
            else RegisterDevicePasswordContent(viewModel, text, cancelDialog, onBack, failureDialog)
        }
    }
}

@Composable
private fun RegisterDevicePasswordContent(
    viewModel: RegisterDeviceViewModel,
    text: RegisterDeviceText,
    cancelDialog: @Composable () -> Unit,
    onBack: () -> Unit,
    failureDialog: @Composable (AuthenticationFailure, onDismiss: () -> Unit) -> Unit,
) {
    val state = viewModel.state
    BackHandler(onBack = onBack)
    cancelDialog()
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = MaterialTheme.wireDimensions.spacing0x,
            title = text.title,
            navigationIconType = NavigationIconType.Close(),
            onNavigationPressed = onBack,
        )
    }) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(padding),
        ) {
            Text(
                text = text.message,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = MaterialTheme.wireDimensions.spacing16x,
                    vertical = MaterialTheme.wireDimensions.spacing24x,
                ).testTag("registerText"),
            )
            RegisterDevicePasswordField(state, viewModel.passwordTextState, text.invalidPasswordMessage)
            Spacer(Modifier.weight(1f))
            WirePrimaryButton(
                text = text.continueLabel,
                onClick = viewModel::onContinue,
                fillMaxWidth = true,
                loading = state.flowState is RegisterDeviceFlowState.Loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.wireDimensions.spacing16x).testTag("registerButton"),
            )
        }
    }
    (state.flowState as? RegisterDeviceFlowState.Error.GenericError)?.let { failure ->
        failureDialog(failure.failure, viewModel::onErrorDismiss)
    }
}

@Composable
private fun RegisterDevicePasswordField(
    state: RegisterDeviceState,
    password: TextFieldState,
    invalidPasswordMessage: String,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = password,
        state = if (state.flowState is RegisterDeviceFlowState.Error.InvalidCredentialsError) {
            WireTextFieldState.Error(invalidPasswordMessage)
        } else WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboard?.hide() },
        modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).testTag("password field"),
        autoFill = true,
    )
}
