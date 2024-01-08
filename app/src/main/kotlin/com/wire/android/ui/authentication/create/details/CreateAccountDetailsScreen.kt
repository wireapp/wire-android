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

package com.wire.android.ui.authentication.create.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.CreatePersonalAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreateTeamAccountNavGraph
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.CreateAccountCodeScreenDestination
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.configuration.server.ServerConfig

@CreatePersonalAccountNavGraph
@CreateTeamAccountNavGraph
@Destination(navArgsDelegate = CreateAccountNavArgs::class)
@Composable
fun CreateAccountDetailsScreen(
    navigator: Navigator,
    createAccountDetailsViewModel: CreateAccountDetailsViewModel = hiltViewModel()
) {
    with(createAccountDetailsViewModel) {
        fun navigateToCodeScreen() = navigator.navigate(
            NavigationCommand(
                CreateAccountCodeScreenDestination(
                    createAccountNavArgs.copy(
                        userRegistrationInfo = createAccountNavArgs.userRegistrationInfo.copy(
                            firstName = detailsState.firstName.text.trim(),
                            lastName = detailsState.lastName.text.trim(),
                            password = detailsState.password.text,
                            teamName = detailsState.teamName.text.trim()
                        )
                    )
                )
            )
        )

        DetailsContent(
            state = detailsState,
            onFirstNameChange = {
                onDetailsChange(
                    it,
                    CreateAccountDetailsViewModel.DetailsFieldType.FirstName
                )
            },
            onLastNameChange = { onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.LastName) },
            onPasswordChange = { onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.Password) },
            onConfirmPasswordChange = {
                onDetailsChange(
                    it,
                    CreateAccountDetailsViewModel.DetailsFieldType.ConfirmPassword
                )
            },
            onTeamNameChange = { onDetailsChange(it, CreateAccountDetailsViewModel.DetailsFieldType.TeamName) },
            onBackPressed = navigator::navigateBack,
            onContinuePressed = { onDetailsContinue(::navigateToCodeScreen) },
            onErrorDismiss = ::onDetailsErrorDismiss,
            serverConfig = serverConfig
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
    WireScaffold(
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
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxHeight()
        ) {
            val keyboardOptions = KeyboardOptions(KeyboardCapitalization.Words, true, KeyboardType.Text, ImeAction.Next)
            val keyboardController = LocalSoftwareKeyboardController.current
            val firstNameFocusRequester = remember { FocusRequester() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
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
                        .focusRequester(firstNameFocusRequester)
                        .testTag("firstName"),
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
                        .testTag("lastName"),
                )

                if (state.type == CreateAccountFlowType.CreateTeam) {
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
                            .testTag("teamName"),
                    )
                }

                WirePasswordTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    labelMandatoryIcon = true,
                    descriptionText = stringResource(R.string.create_account_details_password_description),
                    imeAction = ImeAction.Next,
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        .testTag("password"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError) {
                        WireTextFieldState.Error()
                    } else {
                        WireTextFieldState.Default
                    },
                    autofill = false,
                )

                WirePasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    labelText = stringResource(R.string.create_account_details_confirm_password_label),
                    labelMandatoryIcon = true,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                        .testTag("confirmPassword"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError) when (state.error) {
                        CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_not_matching_error))

                        CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_error))
                    } else WireTextFieldState.Default,
                    autofill = false,
                )
            }

            LaunchedEffect(Unit) {
                firstNameFocusRequester.requestFocus()
                keyboardController?.show()
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background
            ) {
                WirePrimaryButton(
                    modifier = Modifier
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                        .fillMaxWidth(),
                    text = stringResource(R.string.label_continue),
                    onClick = onContinuePressed,
                    fillMaxWidth = true,
                    loading = state.loading,
                    state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                )
            }
        }
    }
    if (state.error is CreateAccountDetailsViewState.DetailsError.DialogError.GenericError) {
        CoreFailureErrorDialog(state.error.coreFailure, onErrorDismiss)
    }
}

@Composable
@Preview
fun PreviewCreateAccountDetailsScreen() {
    DetailsContent(CreateAccountDetailsViewState(CreateAccountFlowType.CreateTeam), {}, {}, {}, {}, {}, {}, {}, {}, ServerConfig.DEFAULT)
}
