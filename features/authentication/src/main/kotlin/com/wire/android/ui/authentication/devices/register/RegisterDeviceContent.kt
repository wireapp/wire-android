/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.authentication.devices.register

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.wire.android.ui.authentication.devices.deviceAuthenticationSlideTransition

data class RegisterDeviceText(
    val title: String,
    val message: String,
    val continueLabel: String,
    val invalidPasswordMessage: String,
)

/** Feature-owned register-device presentation; session cancellation and shared dialogs are slots. */
@Composable
fun <SessionT> RegisterDeviceContent(
    viewModel: RegisterDeviceViewModel<SessionT>,
    text: RegisterDeviceText,
    cancelDialog: @Composable () -> Unit,
    failureDialog: @Composable (AuthenticationFailure, onDismiss: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onSuccess: (RegisterDeviceFlowState.Success<SessionT>) -> Unit,
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
            transitionSpec = { deviceAuthenticationSlideTransition() },
            modifier = modifier.fillMaxSize(),
        ) { needsCode ->
            if (needsCode) {
                RegisterDeviceVerificationCodeScreen(viewModel)
            } else {
                RegisterDevicePasswordContent(viewModel, text, cancelDialog, onBack, failureDialog)
            }
        }
    }
}
