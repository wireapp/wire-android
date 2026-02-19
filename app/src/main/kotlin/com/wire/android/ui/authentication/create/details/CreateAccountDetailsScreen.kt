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

import com.wire.android.navigation.annotation.app.WireCreateTeamAccountDestination
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountCodeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@WireCreateTeamAccountDestination(navArgs = CreateAccountNavArgs::class)
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
                            firstName = firstNameTextState.text.toString().trim(),
                            lastName = lastNameTextState.text.toString().trim(),
                            password = passwordTextState.text.toString(),
                            teamName = teamNameTextState.text.toString().trim()
                        )
                    )
                )
            )
        )

        LaunchedEffect(createAccountDetailsViewModel.detailsState.success) {
            if (createAccountDetailsViewModel.detailsState.success) navigateToCodeScreen()
        }

        DetailsContent(
            state = detailsState,
            firstNameTextState = firstNameTextState,
            lastNameTextState = lastNameTextState,
            passwordTextState = passwordTextState,
            confirmPasswordTextState = confirmPasswordTextState,
            teamNameTextState = teamNameTextState,
            onBackPressed = navigator::navigateBack,
            onContinuePressed = ::onDetailsContinue,
            onErrorDismiss = ::onDetailsErrorDismiss,
            serverConfig = serverConfig
        )
    }
}

@Composable
private fun DetailsContent(
    state: CreateAccountDetailsViewState,
    firstNameTextState: TextFieldState,
    lastNameTextState: TextFieldState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    teamNameTextState: TextFieldState,
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
            val keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
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
                    textState = firstNameTextState,
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
                    textState = lastNameTextState,
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
                        textState = teamNameTextState,
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
                    textState = passwordTextState,
                    labelMandatoryIcon = true,
                    descriptionText = stringResource(R.string.create_account_details_password_description),
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        .testTag("password"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError) {
                        WireTextFieldState.Error()
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
                )

                WirePasswordTextField(
                    textState = confirmPasswordTextState,
                    labelText = stringResource(R.string.create_account_details_confirm_password_label),
                    labelMandatoryIcon = true,
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                    onKeyboardAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                        .testTag("confirmPassword"),
                    state = if (state.error is CreateAccountDetailsViewState.DetailsError.TextFieldError) {
                        when (state.error) {
                        CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_not_matching_error))

                        CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError ->
                            WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_error))
                    }
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
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
@PreviewMultipleThemes
fun PreviewCreateAccountDetailsScreen() = WireTheme {
    DetailsContent(
        state = CreateAccountDetailsViewState(CreateAccountFlowType.CreateTeam),
        firstNameTextState = TextFieldState(),
        lastNameTextState = TextFieldState(),
        passwordTextState = TextFieldState(),
        confirmPasswordTextState = TextFieldState(),
        teamNameTextState = TextFieldState(),
        onBackPressed = {},
        onContinuePressed = {},
        onErrorDismiss = {},
        serverConfig = ServerConfig.DEFAULT
    )
}
