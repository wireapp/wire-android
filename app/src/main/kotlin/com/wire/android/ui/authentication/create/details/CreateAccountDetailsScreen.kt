package com.wire.android.ui.authentication.create.details

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CreateAccountDetailsScreen(viewModel: CreateAccountDetailsViewModel, serverConfig: ServerConfig.Links) {
    clearAutofillTree()
    DetailsContent(
        state = viewModel.detailsState,
        onFirstNameChange = { viewModel.onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.FirstName) },
        onLastNameChange = { viewModel.onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.LastName) },
        onPasswordChange = { viewModel.onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.Password) },
        onConfirmPasswordChange = { viewModel.onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.ConfirmPassword) },
        onTeamNameChange = { viewModel.onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.TeamName) },
        onBackPressed = viewModel::goBackToPreviousStep,
        onContinuePressed = { viewModel.onDetailsContinue() },
        onErrorDismiss = viewModel::onDetailsErrorDismiss,
        serverConfig = serverConfig
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailsContent(
    state: CreateAccountDetailsViewState,
    onFirstNameChange: (TextFieldValue) -> Unit,
    onLastNameChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    onTeamNameChange: (TextFieldValue) -> Unit,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    serverConfig: ServerConfig.Links
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                title = stringResource(id = state.type.titleResId),
                onNavigationPressed = onBackPressed,
                subtitleContent = {
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                }
            )
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.create_personal_account_details_text),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
            )
            NameTextFields(state, onFirstNameChange, onLastNameChange, onTeamNameChange, coroutineScope)
            PasswordTextFields(state, onPasswordChange, onConfirmPasswordChange, coroutineScope)
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x),
            )
        }
    }
    if (state.error is CreateAccountDetailsViewState.DetailsError.DialogError.GenericError)
        CoreFailureErrorDialog(state.error.coreFailure, onErrorDismiss)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NameTextFields(
    state: CreateAccountDetailsViewState,
    onFirstNameChange: (TextFieldValue) -> Unit,
    onLastNameChange: (TextFieldValue) -> Unit,
    onTeamNameChange: (TextFieldValue) -> Unit,
    coroutineScope: CoroutineScope
) {
    val keyboardOptions = KeyboardOptions(KeyboardCapitalization.Words, true, KeyboardType.Text, ImeAction.Next)
    WireTextField(
        value = state.firstName,
        onValueChange = onFirstNameChange,
        placeholderText = stringResource(R.string.create_account_details_first_name_placeholder),
        labelText = stringResource(R.string.create_account_details_first_name_label),
        labelMandatoryIcon = true,
        state = WireTextFieldState.Default,
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing16x
            )
            .testTag("firstName")
            .bringIntoViewOnFocus(coroutineScope)
    )
    WireTextField(
        value = state.lastName,
        onValueChange = onLastNameChange,
        placeholderText = stringResource(R.string.create_account_details_last_name_placeholder),
        labelText = stringResource(R.string.create_account_details_last_name_label),
        labelMandatoryIcon = true,
        state = WireTextFieldState.Default,
        keyboardOptions = keyboardOptions,
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing16x,
                bottom = MaterialTheme.wireDimensions.spacing16x
            )
            .bringIntoViewOnFocus(coroutineScope)
            .testTag("lastName")
    )
    if (state.type == CreateAccountFlowType.CreateTeam)
        WireTextField(
            value = state.teamName,
            onValueChange = onTeamNameChange,
            placeholderText = stringResource(R.string.create_account_details_team_name_placeholder),
            labelText = stringResource(R.string.create_account_details_team_name_label),
            labelMandatoryIcon = true,
            state = WireTextFieldState.Default,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .padding(
                    start = MaterialTheme.wireDimensions.spacing16x,
                    end = MaterialTheme.wireDimensions.spacing16x,
                    bottom = MaterialTheme.wireDimensions.spacing16x
                )
                .bringIntoViewOnFocus(coroutineScope)
                .testTag("teamName")
        )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PasswordTextFields(
    state: CreateAccountDetailsViewState,
    onPasswordChange: (TextFieldValue) -> Unit,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    coroutineScope: CoroutineScope
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        labelMandatoryIcon = true,
        descriptionText = stringResource(R.string.create_account_details_password_description),
        imeAction = ImeAction.Next,
        autofillTypes = listOf(),
        modifier = Modifier
            .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
            .bringIntoViewOnFocus(coroutineScope)
            .testTag("password"),
        state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError){
            WireTextFieldState.Error()
        } else {
            WireTextFieldState.Default
        }
    )
    WirePasswordTextField(
        value = state.confirmPassword,
        onValueChange = onConfirmPasswordChange,
        labelText = stringResource(R.string.create_account_details_confirm_password_label),
        labelMandatoryIcon = true,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        autofillTypes = listOf(),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.wireDimensions.spacing16x,
                vertical = MaterialTheme.wireDimensions.spacing16x
            )
            .bringIntoViewOnFocus(coroutineScope)
            .testTag("confirmPassword"),
        state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError) when (state.error) {
            CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError ->
                WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_not_matching_error))
            CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError ->
                WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_error))
        } else WireTextFieldState.Default
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.bringIntoViewOnFocus(coroutineScope: CoroutineScope): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    return this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { if (it.isFocused) coroutineScope.launch { bringIntoViewRequester.bringIntoView() } }
}

@Composable
@Preview
private fun CreateAccountDetailsScreenPreview() {
    DetailsContent(CreateAccountDetailsViewState(CreateAccountFlowType.CreateTeam), {}, {}, {}, {}, {}, {}, {}, {}, ServerConfig.DEFAULT)
}
