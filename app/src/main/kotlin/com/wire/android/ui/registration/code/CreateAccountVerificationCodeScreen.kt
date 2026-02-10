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

package com.wire.android.ui.registration.code

import com.wire.android.navigation.annotation.app.WireCreateAccountDestination
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.verificationcode.ResendCodeText
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.CodeTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.ramcosta.composedestinations.generated.app.destinations.CreateAccountUsernameScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.job

@WireCreateAccountDestination(
    navArgs = CreateAccountDataNavArgs::class,
    style = AuthPopUpNavigationAnimation::class
)
@Composable
fun CreateAccountVerificationCodeScreen(
    navigator: Navigator,
    createAccountCodeVerificationViewModel: CreateAccountVerificationCodeViewModel = hiltViewModel()
) {
    with(createAccountCodeVerificationViewModel) {
        fun navigateToUsernameScreen() = navigator.navigate(
            NavigationCommand(
                CreateAccountUsernameScreenDestination,
                BackStackMode.CLEAR_WHOLE
            )
        )

        CodeContent(
            state = codeState,
            textState = codeTextState,
            onResendCodePressed = ::resendCode,
            onBackPressed = navigator::navigateBack,
            serverConfig = serverConfig
        )

        (codeState.result as? CreateAccountCodeResult.Error.DialogError)?.let {
            val (title, message) = it.getResources()
            WireDialog(
                title = title,
                text = message,
                onDismiss = ::clearCodeError,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = ::clearCodeError,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
            )
        }
        LaunchedEffect(codeState.result) {
            if (codeState.result is CreateAccountCodeResult.Success) {
                navigateToUsernameScreen()
            }
            if (codeState.result is CreateAccountCodeResult.Error.TooManyDevicesError) {
                clearCodeError()
                clearCodeField()
                navigator.navigate(
                    NavigationCommand(
                        RemoveDeviceScreenDestination,
                        BackStackMode.CLEAR_WHOLE
                    )
                )
            }
        }
        BackHandler {
            if (codeState.loading) return@BackHandler
            navigator.navigateBack()
        }
    }
}

@Composable
private fun CodeContent(
    state: CreateAccountVerificationCodeViewState,
    textState: TextFieldState,
    onResendCodePressed: () -> Unit,
    onBackPressed: () -> Unit,
    serverConfig: ServerConfig.Links
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = stringResource(id = R.string.create_personal_account_title),
                        style = MaterialTheme.wireTypography.title01,
                    )
                    if (serverConfig.isOnPremises == true) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                },
                canNavigateBack = true,
                onNavigateBack = {
                    if (state.loading) return@NewAuthHeader
                    onBackPressed()
                }
            )
        },
        contentPadding = dimensions().spacing16x,
        content = {
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CodeTextField(
                        codeLength = state.codeLength,
                        textState = textState,
                        state = when (state.result) {
                            is CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError ->
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
                    VerticalSpace.x16()
                    ResendCodeText(
                        onResendCodePressed = onResendCodePressed,
                        clickEnabled = !state.loading
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
    )
}

@Composable
private fun CreateAccountCodeResult.Error.DialogError.getResources() = when (this) {
    CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_already_in_use_error)
    )

    CreateAccountCodeResult.Error.DialogError.BlackListedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_blacklisted_error)
    )

    CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_domain_blocked_error)
    )

    CreateAccountCodeResult.Error.DialogError.InvalidEmailError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_email_invalid_error)
    )

    CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_code_error_team_members_limit_reached)
    )

    CreateAccountCodeResult.Error.DialogError.CreationRestrictedError -> DialogErrorStrings(
        stringResource(id = R.string.create_account_code_error_title),
        stringResource(id = R.string.create_account_code_error_personal_account_creation_restricted)
    )
    // TODO: sync with design about the error message
    CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError ->
        DialogErrorStrings("User Already LoggedIn", "UserAlreadyLoggedIn")

    is CreateAccountCodeResult.Error.DialogError.GenericError ->
        this.coreFailure.dialogErrorStrings(LocalContext.current.resources)
}

@Composable
@Preview
fun PreviewCreateAccountVerificationCodeScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            CodeContent(
                textState = TextFieldState(),
                state = CreateAccountVerificationCodeViewState(),
                onResendCodePressed = {},
                onBackPressed = {},
                serverConfig = ServerConfig.DEFAULT
            )
        }
    }
}
