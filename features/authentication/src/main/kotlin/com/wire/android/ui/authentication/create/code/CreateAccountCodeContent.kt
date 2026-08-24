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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.wire.android.ui.authentication.create.code

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.ui.authentication.verificationcode.ResendCodeText
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.job

@Immutable
data class CreateAccountCodePresentation(
    val title: String,
    val codeInstruction: String,
    val invalidActivationCodeError: String,
    @StringRes val backContentDescription: Int,
)

@Suppress("LongParameterList")
@Composable
fun <FlowT, UserT, FailureT> CreateAccountCodeContent(
    state: CreateAccountCodeViewState<FlowT, UserT, FailureT>,
    presentation: CreateAccountCodePresentation,
    textState: TextFieldState,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = presentation.title,
                onNavigationPressed = onBackPressed,
                subtitleContent = subtitleContent,
                navigationIconType = NavigationIconType.Back(presentation.backContentDescription),
            )
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(internalPadding),
        ) {
            Text(
                text = presentation.codeInstruction,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CodeTextField(
                    codeLength = state.codeLength,
                    textState = textState,
                    state = when (state.result) {
                        is CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError ->
                            WireTextFieldState.Error(presentation.invalidActivationCodeError)

                        else -> WireTextFieldState.Default
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                )
                AnimatedVisibility(visible = state.loading) {
                    WireCircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.primary,
                        size = MaterialTheme.wireDimensions.spacing24x,
                        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing16x),
                    )
                }
                ResendCodeText(
                    onResendCodePressed = onResendCodePressed,
                    clickEnabled = !state.loading,
                    timerText = state.remainingTimerText,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        LaunchedEffect(Unit) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCreateAccountCodeContent() = WireTheme {
    CreateAccountCodeContent(
        state = CreateAccountCodeViewState<Unit, Nothing, Nothing>(type = Unit),
        presentation = CreateAccountCodePresentation(
            title = "Create account",
            codeInstruction = "Check your email for the verification code and enter it below.",
            invalidActivationCodeError = "Invalid code",
            backContentDescription = CommonR.string.content_description_left_arrow,
        ),
        textState = TextFieldState(),
        onResendCodePressed = {},
        onBackPressed = {},
    )
}
