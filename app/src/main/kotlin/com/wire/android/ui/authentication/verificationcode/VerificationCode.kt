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

package com.wire.android.ui.authentication.verificationcode

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.job

@Composable
fun VerificationCode(
    codeLength: Int,
    codeState: TextFieldState,
    isLoading: Boolean,
    isCurrentCodeInvalid: Boolean,
    onResendCode: () -> Unit,
    modifier: Modifier = Modifier,
    showLoadingProgress: Boolean = true,
    timerText: String? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {

        val state = if (isCurrentCodeInvalid) WireTextFieldState.Error(
            stringResource(id = R.string.second_factor_code_error)
        )
        else WireTextFieldState.Default

        CodeTextField(
            codeLength = codeLength,
            textState = codeState,
            state = state,
            modifier = Modifier.focusRequester(focusRequester)
        )

        Crossfade(
            targetState = isLoading to showLoadingProgress,
            modifier = Modifier
                .padding(top = MaterialTheme.wireDimensions.spacing24x)
                .animateContentSize(),
        ) { (isLoading, showLoadingProgress) ->
            when {
                !isLoading ->
                    ResendCodeText(
                        onResendCodePressed = onResendCode,
                        clickEnabled = true,
                        timerText = timerText,
                        modifier = Modifier
                            .defaultMinSize(minHeight = MaterialTheme.wireDimensions.spacing24x)
                            .wrapContentHeight(align = Alignment.CenterVertically),
                    )

                isLoading && showLoadingProgress -> WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.primary,
                    size = MaterialTheme.wireDimensions.spacing24x,
                )
            }
        }

        LaunchedEffect(Unit) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewVerificationCode() = WireTheme {
    VerificationCode(
        codeLength = 6,
        codeState = TextFieldState(),
        isLoading = false,
        isCurrentCodeInvalid = false,
        onResendCode = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewVerificationCodeTimer() = WireTheme {
    VerificationCode(
        codeLength = 6,
        codeState = TextFieldState(),
        isLoading = false,
        isCurrentCodeInvalid = false,
        onResendCode = {},
        timerText = "00:30",
    )
}
