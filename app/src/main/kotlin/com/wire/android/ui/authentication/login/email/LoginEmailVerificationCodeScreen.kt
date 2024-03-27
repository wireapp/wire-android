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

package com.wire.android.ui.authentication.login.email

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.verificationcode.VerificationCode
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
fun LoginEmailVerificationCodeScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    viewModel: LoginEmailViewModel = hiltViewModel()
) = LoginEmailVerificationCodeContent(
    viewModel.secondFactorVerificationCodeState,
    viewModel.loginState.emailLoginLoading,
    { viewModel.onCodeChange(it, onSuccess) },
    viewModel::onCodeResend,
    viewModel::onCodeVerificationBackPress
)

@Composable
private fun LoginEmailVerificationCodeContent(
    verificationCodeState: VerificationCodeState,
    isLoading: Boolean,
    onCodeChange: (CodeFieldValue) -> Unit,
    onCodeResend: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler { onBackPressed() }
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = R.string.second_factor_authentication_title),
                navigationIconType = null,
            )
        }
    ) { internalPadding ->
        MainContent(
            codeState = verificationCodeState,
            isLoading = isLoading,
            onCodeChange = onCodeChange,
            onResendCode = onCodeResend,
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .padding(dimensions().spacing16x)
        )
    }
}

@Composable
private fun MainContent(
    codeState: VerificationCodeState,
    isLoading: Boolean,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCode: () -> Unit,
    modifier: Modifier = Modifier
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
) {
    Text(
        text = UIText.StringResource(
            R.string.second_factor_authentication_instructions_label,
            codeState.emailUsed
        ).asString(),
        color = colorsScheme().onBackground,
        style = typography().body01,
        textAlign = TextAlign.Start
    )
    Spacer(modifier = Modifier
        .height(dimensions().spacing8x)
        .weight(1f))
    VerificationCode(
        codeLength = codeState.codeLength,
        currentCode = codeState.codeInput.text,
        isLoading = isLoading,
        isCurrentCodeInvalid = codeState.isCurrentCodeInvalid,
        onCodeChange = onCodeChange,
        onResendCode = onResendCode,
    )
    Spacer(modifier = Modifier
        .height(dimensions().spacing8x)
        .weight(1f))
}

@PreviewMultipleThemes
@Composable
internal fun LoginEmailVerificationCodeScreenPreview() = WireTheme {
    LoginEmailVerificationCodeContent(
        VerificationCodeState(
            codeLength = 6,
            codeInput = CodeFieldValue(TextFieldValue("12"), false),
            isCurrentCodeInvalid = false,
            emailUsed = ""
        ),
        false,
        {},
        {},
        {},
    )
}
