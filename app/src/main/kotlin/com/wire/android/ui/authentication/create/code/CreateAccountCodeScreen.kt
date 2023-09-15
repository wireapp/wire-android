/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.create.code

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.common.CreatePersonalAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreateTeamAccountNavGraph
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryNavArgs
import com.wire.android.ui.authentication.verificationcode.ResendCodeText
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.CreateAccountSummaryScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.server.ServerConfig

@CreatePersonalAccountNavGraph
@CreateTeamAccountNavGraph
@Destination(navArgsDelegate = CreateAccountNavArgs::class)
@Composable
fun CreateAccountCodeScreen(
    navigator: Navigator,
    createAccountCodeViewModel: CreateAccountCodeViewModel = hiltViewModel()
) {
    with(createAccountCodeViewModel) {
        fun navigateToSummaryScreen() = navigator.navigate(
            NavigationCommand(
                CreateAccountSummaryScreenDestination(CreateAccountSummaryNavArgs(createAccountNavArgs.flowType)),
                BackStackMode.CLEAR_WHOLE
            )
        )

        CodeContent(
            state = codeState,
            onCodeChange = { onCodeChange(it, ::navigateToSummaryScreen) },
            onResendCodePressed = ::resendCode,
            onBackPressed = navigator::navigateBack,
            onErrorDismiss = ::clearCodeError,
            onRemoveDeviceOpen = {
                clearCodeError()
                clearCodeField()
                navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
            },
            serverConfig = serverConfig
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CodeContent(
    state: CreateAccountCodeViewState,
    onCodeChange: (CodeFieldValue) -> Unit,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    serverConfig: ServerConfig.Links
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
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
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(internalPadding)
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
fun PreviewCreateAccountCodeScreen() {
    CodeContent(CreateAccountCodeViewState(CreateAccountFlowType.CreatePersonalAccount), {}, {}, {}, {}, {}, ServerConfig.DEFAULT)
}
