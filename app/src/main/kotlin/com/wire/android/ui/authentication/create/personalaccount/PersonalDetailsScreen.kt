package com.wire.android.ui.authentication.create.personalaccount

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(viewModel: CreatePersonalAccountViewModel) {
    DetailsContent(
        state = viewModel.state.details,
        onFirstNameChange = { viewModel.onDetailsChange(it, DetailsFieldType.FirstName) },
        onLastNameChange = { viewModel.onDetailsChange(it, DetailsFieldType.LastName) },
        onPasswordChange = { viewModel.onDetailsChange(it, DetailsFieldType.Password) },
        onConfirmPasswordChange = { viewModel.onDetailsChange(it, DetailsFieldType.ConfirmPassword) },
        onBackPressed = viewModel::goBackToPreviousStep,
        onContinuePressed = viewModel::onDetailsContinue,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DetailsContent(
    state: CreatePersonalAccountViewState.Details,
    onFirstNameChange: (TextFieldValue) -> Unit,
    onLastNameChange: (TextFieldValue) -> Unit,
    onPasswordChange: (TextFieldValue) -> Unit,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.appBarElevation(),
                title = stringResource(R.string.create_personal_account_title),
                onNavigationPressed = onBackPressed
            )
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxHeight().verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.create_personal_account_details_text),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.fillMaxWidth().padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                )
            )
            NameTextFields(state, onFirstNameChange, onLastNameChange, coroutineScope)
            PasswordTextFields(state, onPasswordChange, onConfirmPasswordChange, coroutineScope)
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.wireDimensions.spacing16x),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NameTextFields(
    state: CreatePersonalAccountViewState.Details,
    onFirstNameChange: (TextFieldValue) -> Unit,
    onLastNameChange: (TextFieldValue) -> Unit,
    coroutineScope: CoroutineScope
) {
    WireTextField(
        value = state.firstName,
        onValueChange = onFirstNameChange,
        placeholderText = stringResource(R.string.create_personal_account_details_first_name_placeholder),
        labelText = stringResource(R.string.create_personal_account_details_first_name_label),
        labelMandatoryIcon = true,
        state = WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).bringIntoViewOnFocus(coroutineScope)
    )
    WireTextField(
        value = state.lastName,
        onValueChange = onLastNameChange,
        placeholderText = stringResource(R.string.create_personal_account_details_last_name_placeholder),
        labelText = stringResource(R.string.create_personal_account_details_last_name_label),
        labelMandatoryIcon = true,
        state = WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.spacing16x,
                vertical = MaterialTheme.wireDimensions.spacing16x
            ).bringIntoViewOnFocus(coroutineScope)
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PasswordTextFields(
    state: CreatePersonalAccountViewState.Details,
    onPasswordChange: (TextFieldValue) -> Unit,
    onConfirmPasswordChange: (TextFieldValue) -> Unit,
    coroutineScope: CoroutineScope
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        descriptionText = stringResource(R.string.create_personal_account_details_password_description),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Next),
        modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x).bringIntoViewOnFocus(coroutineScope),
        state = if(state.error is CreatePersonalAccountViewState.DetailsError.None) WireTextFieldState.Default
                else WireTextFieldState.Error()
    )
    WirePasswordTextField(
        value = state.confirmPassword,
        onValueChange = onConfirmPasswordChange,
        labelText = stringResource(R.string.create_personal_account_details_confirm_password_label),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.spacing16x,
                vertical = MaterialTheme.wireDimensions.spacing16x
            ).bringIntoViewOnFocus(coroutineScope),
        state = when(state.error) {
            CreatePersonalAccountViewState.DetailsError.None ->  WireTextFieldState.Default
            CreatePersonalAccountViewState.DetailsError.PasswordsNotMatchingError ->
                WireTextFieldState.Error(stringResource(id = R.string.create_personal_account_details_password_not_matching_error))
            CreatePersonalAccountViewState.DetailsError.InvalidPasswordError ->
                WireTextFieldState.Error(stringResource(id = R.string.create_personal_account_details_password_error))
        }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.bringIntoViewOnFocus(coroutineScope: CoroutineScope): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    return this.bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { if(it.isFocused) coroutineScope.launch { bringIntoViewRequester.bringIntoView() } }
}

@Composable
@Preview
private fun DetailsScreenPreview() { DetailsContent(CreatePersonalAccountViewState.Details(), {}, {}, {}, {}, {}, {},) }
