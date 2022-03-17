package com.wire.android.ui.authentication.create.code

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CreateAccountCodeScreen(viewModel: CreateAccountCodeViewModel) {
    CodeContent(
        state = viewModel.codeState,
        onCodeChange = viewModel::onCodeChange,
        onResendCodePressed = viewModel::resendCode,
        onBackPressed = viewModel::goBackToPreviousStep
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun CodeContent(
    state: CreateAccountCodeViewState,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = state.type.titleResId),
            onNavigationPressed = onBackPressed
        )
    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = stringResource(R.string.create_account_code_text, state.email),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally,) {
                CodeTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    state = when {
                        state.loading -> WireTextFieldState.Disabled
                        state.error is CreateAccountCodeViewState.CodeError.InvalidCodeError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_code_error))
                        else -> WireTextFieldState.Default
                    },
                )
                AnimatedVisibility(visible = state.loading) {
                    WireCircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.primary,
                        size = MaterialTheme.wireDimensions.spacing24x,
                        modifier = Modifier.padding(vertical = MaterialTheme.wireDimensions.spacing16x)
                    )
                }
                ResendCodeText(onResendCodePressed = onResendCodePressed, clickEnabled = !state.loading)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResendCodeText(onResendCodePressed: () -> Unit, clickEnabled: Boolean) {
    Text(
        text = stringResource(R.string.create_account_code_resend),
        style = MaterialTheme.wireTypography.body02.copy(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = clickEnabled,
                onClick = onResendCodePressed
            )
            .padding(
                horizontal = MaterialTheme.wireDimensions.spacing16x,
                vertical = MaterialTheme.wireDimensions.spacing24x
            )
    )
}

@Composable
@Preview
private fun CreateAccountCodeScreenPreview() {
    CodeContent(CreateAccountCodeViewState(CreateAccountFlowType.CreatePersonalAccount), {}, {}, {})
}
