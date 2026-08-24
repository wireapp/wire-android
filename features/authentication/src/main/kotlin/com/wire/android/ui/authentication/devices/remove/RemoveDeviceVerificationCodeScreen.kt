/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.devices.remove

import androidx.compose.runtime.Composable
import com.wire.android.ui.authentication.verificationcode.VerificationCodeScreenContent

@Composable
fun <DeviceT> RemoveDeviceVerificationCodeScreen(viewModel: RemoveDeviceViewModel<DeviceT>) =
    VerificationCodeScreenContent(
        viewModel.secondFactorVerificationCodeTextState,
        viewModel.secondFactorVerificationCodeState,
        viewModel.state.is2FAInProgress,
        viewModel::onCodeResend,
        viewModel::onCodeVerificationBackPress,
    )
