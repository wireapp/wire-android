/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.NewLoginNavGraph
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.destinations.NewLoginPasswordScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes

@NewLoginNavGraph(start = true)
@WireDestination(
    style = AuthPopUpNavigationAnimation::class,
    navArgsDelegate = LoginNavArgs::class,
)
@Composable
fun NewLoginScreen(
    navigator: Navigator,
    viewModel: NewLoginViewModel = hiltViewModel()
) {
    DomainCheckupDialog(viewModel.state, navigator, viewModel::onDismissDialog)
    LoginContent(
        loginEmailSSOState = viewModel.state,
        userIdentifierState = viewModel.userIdentifierTextState,
        onNextClicked = {
            viewModel.onLoginStarted { loginPathRedirect ->
                when (val newLoginDestination: NewLoginDestination = loginPathRedirect.toPasswordOrSsoDestination()) {
                    is NewLoginDestination.EmailPassword -> {
                        navigator.navigate(
                            NavigationCommand(
                                NewLoginPasswordScreenDestination(
                                    LoginNavArgs(
                                        userHandle = viewModel.userIdentifierTextState.text.toString(),
                                        loginPasswordPath = newLoginDestination.loginPasswordPath
                                    )
                                )
                            )
                        )
                    }

                    else -> {
                        TODO("navigate to SSO screen")
                    }
                }
            }
        },
        canNavigateBack = navigator.navController.previousBackStackEntry != null, // if there is a previous screen to navigate back to
        navigateBack = navigator::navigateBack,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginContent(
    loginEmailSSOState: NewLoginScreenState,
    userIdentifierState: TextFieldState,
    onNextClicked: () -> Unit,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
) {
    NewLoginContainer(
        header = {
            NewLoginHeader(
                title = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = dimensions().spacing32x)
                            .size(dimensions().spacing120x)
                    )
                    NewLoginSubtitle(
                        title = stringResource(R.string.enterprise_login_welcome),
                        modifier = Modifier.padding(top = dimensions().spacing16x)
                    )
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = navigateBack,
            )
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = dimensions().spacing16x)
                    .semantics {
                        testTagsAsResourceId = true
                    }
            ) {
                val error = when (loginEmailSSOState.flowState) {
                    is DomainCheckupState.Error.TextFieldError.InvalidValue ->
                        stringResource(R.string.enterprise_login_error_invalid_user_identifier)

                    else -> null
                }
                EmailOrSSOCodeInput(userIdentifierState, error)
                VerticalSpace.x8()
                LoginNextButton(
                    loading = loginEmailSSOState.flowState is DomainCheckupState.Loading,
                    enabled = loginEmailSSOState.nextEnabled,
                    onClick = onNextClicked,
                )
            }
        }
    }
}

@Composable
private fun LoginNextButton(
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.enterprise_login_next),
            onClick = onClick,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton")
        )
    }
}

@Composable
private fun EmailOrSSOCodeInput(
    userIdentifierState: TextFieldState,
    error: String?
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.enterprise_login_user_identifier_label_placeholder),
        labelText = stringResource(R.string.enterprise_login_user_identifier_label),
        state = when {
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        semanticDescription = stringResource(R.string.content_description_enterprise_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = Modifier.testTag("emailField"),
        testTag = "userIdentifierInput",
    )
}

@Composable
fun DomainCheckupDialog(loginEmailSSOState: NewLoginScreenState, navigator: Navigator, onDismiss: () -> Unit) {
    val resources = LocalContext.current.resources
    when (val state = loginEmailSSOState.flowState) {
        is DomainCheckupState.Error.DialogError.GenericError -> DomainCheckupDialogs(
            dialogErrorStrings = state.coreFailure.dialogErrorStrings(resources), onDismiss = onDismiss
        )

        is DomainCheckupState.Error.DialogError.NotSupported -> navigator.navigate(NavigationCommand(WelcomeScreenDestination()))
        else -> {
            /* do nothing */
        }
    }
}

@Composable
fun DomainCheckupDialogs(dialogErrorStrings: DialogErrorStrings, onDismiss: () -> Unit) {
    WireDialog(
        title = dialogErrorStrings.title,
        text = dialogErrorStrings.annotatedMessage,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(R.string.label_ok),
            onClick = onDismiss,
            type = WireDialogButtonType.Primary
        ),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewNewLoginScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginContent(
                loginEmailSSOState = NewLoginScreenState(),
                userIdentifierState = TextFieldState(),
                onNextClicked = {},
                canNavigateBack = false,
                navigateBack = {},
            )
        }
    }
}
