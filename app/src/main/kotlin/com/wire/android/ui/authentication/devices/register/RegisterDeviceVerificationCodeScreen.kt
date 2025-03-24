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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.authentication.verificationcode.VerificationCodeScreenContent
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun RegisterDeviceVerificationCodeScreen(
    viewModel: RegisterDeviceViewModel = hiltViewModel()
) = VerificationCodeScreenContent(
    viewModel.secondFactorVerificationCodeTextState,
    viewModel.secondFactorVerificationCodeState,
    viewModel.state.flowState is RegisterDeviceFlowState.Loading,
    viewModel::onCodeResend,
    viewModel::onCodeVerificationBackPress
)

@PreviewMultipleThemes
@Composable
internal fun RegisterDeviceVerificationCodeScreenPreview() = WireTheme {
    VerificationCodeScreenContent(
        verificationCodeTextState = TextFieldState(),
        verificationCodeState = VerificationCodeState(
            codeLength = 6,
            isCurrentCodeInvalid = false,
            emailUsed = ""
        ),
        isLoading = false,
        onCodeResend = {},
        onBackPressed = {},
    )
}
