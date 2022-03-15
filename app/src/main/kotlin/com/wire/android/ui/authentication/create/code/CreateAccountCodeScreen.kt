package com.wire.android.ui.authentication.create.code

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CreateAccountCodeScreen(viewModel: CreateAccountCodeViewModel, title: String) {
    CodeContent(
        state = viewModel.codeState,
        title = title,
        onCodeChange = viewModel::onCodeChange,
        onResendCodePressed = viewModel::resendCode,
        onBackPressed = viewModel::goBackToPreviousStep
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeContent(
    state: CreateAccountCodeViewState,
    title: String,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(topBar = { WireCenterAlignedTopAppBar(elevation = 0.dp, title = title, onNavigationPressed = onBackPressed) }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                text = stringResource(R.string.create_personal_account_code_text, state.email),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.fillMaxWidth().padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally,) {
                CodeTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    state = when(state.error) {
                        CreateAccountCodeViewState.CodeError.InvalidCodeError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_personal_account_code_error))
                        CreateAccountCodeViewState.CodeError.None -> WireTextFieldState.Default
                    }
                )
                ResendCodeText(onResendCodePressed = onResendCodePressed)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResendCodeText(onResendCodePressed: () -> Unit) {
    Text(
        text = stringResource(R.string.create_personal_account_code_resend),
        style = MaterialTheme.wireTypography.body02.copy(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.primary
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.wireDimensions.spacing16x,
                vertical = MaterialTheme.wireDimensions.spacing24x
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onResendCodePressed
            )
    )
}

@Composable
@Preview
private fun CreateAccountCodeScreenPreview() {
    CodeContent(CreateAccountCodeViewState(), "title", {}, {}, {})
}
