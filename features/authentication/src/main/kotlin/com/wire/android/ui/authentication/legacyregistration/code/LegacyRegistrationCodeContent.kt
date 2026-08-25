/* Wire Copyright (C) 2026 Wire Swiss GmbH */
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.authentication.legacyregistration.code

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.wire.android.ui.authentication.verificationcode.ResendCodeText
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.job

data class LegacyRegistrationCodeText(
    val title: String,
    val instruction: String,
    val invalidCode: String,
)

@Composable
fun <UserT, FailureT> LegacyRegistrationCodeContent(
    state: LegacyRegistrationCodeState<UserT, FailureT>,
    textState: TextFieldState,
    text: LegacyRegistrationCodeText,
    serverTitle: @Composable () -> Unit,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    NewAuthContainer(
        modifier = modifier,
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = text.title,
                        style = MaterialTheme.wireTypography.title01,
                        modifier = Modifier.semantics { heading() },
                    )
                    serverTitle()
                },
                canNavigateBack = true,
                onNavigateBack = { if (!state.loading) onBackPressed() },
            )
        },
        contentPadding = dimensions().spacing16x,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
            Text(
                text.instruction,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = MaterialTheme.wireDimensions.spacing16x,
                    vertical = MaterialTheme.wireDimensions.spacing24x,
                ),
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CodeTextField(
                    codeLength = state.codeLength,
                    textState = textState,
                    state = if (state.result is LegacyRegistrationCodeState.Result.InvalidActivationCode) {
                        WireTextFieldState.Error(text.invalidCode)
                    } else {
                        WireTextFieldState.Default
                    },
                    modifier = Modifier.focusRequester(focusRequester),
                )
                AnimatedVisibility(state.loading) {
                    WireCircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.primary,
                        size = MaterialTheme.wireDimensions.spacing24x,
                        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing16x),
                    )
                }
                VerticalSpace.x16()
                ResendCodeText(onResendCodePressed, clickEnabled = !state.loading)
            }
            Spacer(Modifier.weight(1f))
        }
        LaunchedEffect(Unit) {
            coroutineContext.job.invokeOnCompletion {
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
    }
}
