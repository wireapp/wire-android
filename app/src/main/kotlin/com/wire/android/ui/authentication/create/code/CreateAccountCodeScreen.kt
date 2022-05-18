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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.ServerConfig

@Composable
fun CreateAccountCodeScreen(viewModel: CreateAccountCodeViewModel, serverConfig: ServerConfig) {
    CodeContent(
        state = viewModel.codeState,
        onCodeChange = { viewModel.onCodeChange(it, serverConfig) },
        onResendCodePressed = { viewModel.resendCode(serverConfig) },
        onBackPressed = viewModel::goBackToPreviousStep,
        onErrorDismiss = viewModel::onCodeErrorDismiss,
        onRemoveDeviceOpen = viewModel::onTooManyDevicesError
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun CodeContent(
    state: CreateAccountCodeViewState,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = state.type.titleResId),
            onNavigationPressed = onBackPressed
        )
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight().padding(internalPadding)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CodeTextField(
                    value = state.code.text,
                    onValueChange = onCodeChange,
                    state = when (state.error) {
                        is CreateAccountCodeViewState.CodeError.TextFieldError.InvalidActivationCodeError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_code_error))
                        else -> WireTextFieldState.Default
                    },
                    modifier = Modifier.focusRequester(focusRequester)
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
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    if (state.error is CreateAccountCodeViewState.CodeError.DialogError) {
        val (title, message) = state.error.getResources(type = state.type)
        WireDialog(
            title = title,
            text = message,
            onDismiss = onErrorDismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onErrorDismiss,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        )
    } else if (state.error is CreateAccountCodeViewState.CodeError.TooManyDevicesError) {
        onRemoveDeviceOpen()
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
private fun CreateAccountCodeViewState.CodeError.DialogError.getResources(type: CreateAccountFlowType) = when (this) {
    CreateAccountCodeViewState.CodeError.DialogError.AccountAlreadyExistsError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_already_in_use_error)
    )
    CreateAccountCodeViewState.CodeError.DialogError.BlackListedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_blacklisted_error)
    )
    CreateAccountCodeViewState.CodeError.DialogError.EmailDomainBlockedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_domain_blocked_error)
    )
    CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_invalid_error)
    )
    CreateAccountCodeViewState.CodeError.DialogError.TeamMembersLimitError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_code_error_team_members_limit_reached)
    )
    CreateAccountCodeViewState.CodeError.DialogError.CreationRestrictedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(
            id = when (type) {
                CreateAccountFlowType.CreatePersonalAccount -> R.string.create_account_code_error_personal_account_creation_restricted
                CreateAccountFlowType.CreateTeam -> R.string.create_account_code_error_team_creation_restricted
            }
        )
    )
    // TODO: sync with design about the error message
    CreateAccountCodeViewState.CodeError.DialogError.UserAlreadyExists ->
        DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")
    is CreateAccountCodeViewState.CodeError.DialogError.GenericError ->
        this.coreFailure.dialogErrorStrings(LocalContext.current.resources)
}

@Composable
@Preview
private fun CreateAccountCodeScreenPreview() {
    CodeContent(CreateAccountCodeViewState(CreateAccountFlowType.CreatePersonalAccount), {}, {}, {}, {}, {})
}
