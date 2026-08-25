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
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.wire.android.R
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.login.sso.SsoIdentityChangedDialog
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.authentication.BackendConfigSuccessContent
import com.wire.android.ui.authentication.MissingBackendConfigContent
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

/**
 * Navigation-neutral adapter. Cross-flow effects are exposed as semantic [NewLoginAction] values.
 */
@Composable
internal fun NewLoginRouteScreen(
    navArgs: LoginNavArgs,
    viewModel: NewLoginViewModel,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
    onAction: (NewLoginAction) -> Unit,
) {
    // Handle SSO code auto-login from intent parameter
    LaunchedEffect(navArgs.ssoCodeAutoLogin) {
        navArgs.ssoCodeAutoLogin?.let {
            // Pre-fill the SSO code in the user identifier field
            viewModel.userIdentifierTextState.setTextAndPlaceCursorAtEnd(it.ssoCode)

            // Auto-initiate login if flag is set
            if (it.autoInitiateLogin) {
                viewModel.onLoginStarted()
            }
        }
    }
    (viewModel.state.flowState as? NewLoginFlowState.CustomConfigDialog)?.let { customServerDialogState ->
        CustomServerDetailsDialog(
            serverLinks = customServerDialogState.serverLinks,
            onDismiss = viewModel::onDismissDialog,
            onConfirm = {
                viewModel.onCustomServerDialogConfirm(customServerDialogState.serverLinks)
            }
        )
    }
    (viewModel.state.flowState as? NewLoginFlowState.Error.DialogError)?.let { dialogErrorState ->
        LoginErrorDialog(dialogErrorState.toLoginDialogErrorData(), viewModel::onDismissDialog)
    }
    if (viewModel.state.flowState == NewLoginFlowState.SsoIdentityChanged) {
        SsoIdentityChangedDialog(
            onDismiss = viewModel::onSsoIdentityChangeDismissed,
            onConfirm = viewModel::onSsoIdentityChangeConfirmed,
        )
    }
    LoginContent(
        loginEmailSSOState = viewModel.state,
        userIdentifierState = viewModel.userIdentifierTextState,
        serverConfig = viewModel.serverConfig,
        onNextClicked = {
            viewModel.onLoginStarted()
        },
        onBackendConfigLinkEntered = viewModel::onBackendConfigLinkEntered,
        onBackendConfigSuccessContinue = viewModel::onBackendConfigSuccessContinue,
        onNoBackendSelected = viewModel::onNoBackendSelected,
        canNavigateBack = canNavigateBack,
        navigateBack = navigateBack,
    )

    HandleActions(viewModel.actions, onAction)
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("CyclomaticComplexMethod")
@Composable
private fun LoginContent(
    loginEmailSSOState: NewLoginScreenState,
    userIdentifierState: TextFieldState,
    serverConfig: ServerConfig.Links,
    canNavigateBack: Boolean,
    onNextClicked: () -> Unit,
    onBackendConfigLinkEntered: (String) -> Unit,
    onBackendConfigSuccessContinue: () -> Unit,
    onNoBackendSelected: () -> Unit = {},
    navigateBack: () -> Unit,
) {
    val isBackendConfigSetup = loginEmailSSOState.flowState == NewLoginFlowState.MissingBackendConfig ||
            loginEmailSSOState.flowState == NewLoginFlowState.LoadingBackendConfig ||
            loginEmailSSOState.flowState == NewLoginFlowState.BackendConfigError
    val isBackendConfigSuccess = loginEmailSSOState.flowState == NewLoginFlowState.BackendConfigSuccess
    val showCredentialsSubtitle = !isBackendConfigSetup && !isBackendConfigSuccess
    NewAuthContainer(
        showBackendSelector = true,
        onNoBackendSelected = onNoBackendSelected,
        header = {
            NewAuthHeader(
                title = {
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = typography().title01,
                            textColor = colorsScheme().onSurface,
                            titleResId = R.string.enterprise_login_on_prem_welcome_title,
                            modifier = if (showCredentialsSubtitle) {
                                Modifier.padding(bottom = dimensions().spacing24x)
                            } else {
                                Modifier
                            },
                        )
                        if (showCredentialsSubtitle) {
                            NewAuthSubtitle(
                                title = stringResource(id = R.string.enterprise_login_credentials_title),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(horizontal = dimensions().spacing32x)
                                .size(dimensions().spacing120x)
                        )
                        NewAuthSubtitle(
                            title = when {
                                isBackendConfigSetup -> stringResource(R.string.missing_backend_config_title)
                                isBackendConfigSuccess -> ""
                                else -> stringResource(R.string.enterprise_login_welcome)
                            },
                            modifier = Modifier.padding(top = dimensions().spacing16x)
                        )
                    }
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
                if (isBackendConfigSuccess) {
                    BackendConfigSuccessContent(onContinue = onBackendConfigSuccessContinue)
                } else if (isBackendConfigSetup) {
                    MissingBackendConfigContent(
                        errorText = if (loginEmailSSOState.flowState == NewLoginFlowState.BackendConfigError) {
                            stringResource(R.string.missing_backend_config_error)
                        } else {
                            null
                        },
                        isLoading = loginEmailSSOState.flowState == NewLoginFlowState.LoadingBackendConfig,
                        onConfigurationLinkEntered = onBackendConfigLinkEntered,
                    )
                } else {
                    val error = when (loginEmailSSOState.flowState) {
                        is NewLoginFlowState.Error.TextFieldError.InvalidValue ->
                            stringResource(R.string.enterprise_login_error_invalid_user_identifier)

                        else -> null
                    }
                    EmailOrSSOCodeInput(userIdentifierState, error)
                    VerticalSpace.x8()
                    LoginNextButton(
                        loading = loginEmailSSOState.flowState is NewLoginFlowState.Loading,
                        enabled = loginEmailSSOState.nextEnabled,
                        onClick = onNextClicked,
                    )
                }
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

@PreviewMultipleThemes
@Composable
fun PreviewNewLoginScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginContent(
                loginEmailSSOState = NewLoginScreenState(),
                userIdentifierState = TextFieldState(),
                serverConfig = ServerConfig.DEFAULT.copy(isOnPremises = false),
                onNextClicked = {},
                onBackendConfigLinkEntered = {},
                onBackendConfigSuccessContinue = {},
                canNavigateBack = false,
                navigateBack = {},
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNewLoginScreenCustomConfig() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginContent(
                loginEmailSSOState = NewLoginScreenState(),
                userIdentifierState = TextFieldState(),
                serverConfig = ServerConfig.DEFAULT.copy(isOnPremises = true),
                onNextClicked = {},
                onBackendConfigLinkEntered = {},
                onBackendConfigSuccessContinue = {},
                canNavigateBack = false,
                navigateBack = {},
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNewLoginScreenMissingBackendConfig() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginContent(
                loginEmailSSOState = NewLoginScreenState(flowState = NewLoginFlowState.MissingBackendConfig),
                userIdentifierState = TextFieldState(),
                serverConfig = ServerConfig.DEFAULT.copy(isOnPremises = false),
                onNextClicked = {},
                onBackendConfigLinkEntered = {},
                onBackendConfigSuccessContinue = {},
                canNavigateBack = false,
                navigateBack = {},
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNewLoginScreenBackendConfigSuccess() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginContent(
                loginEmailSSOState = NewLoginScreenState(flowState = NewLoginFlowState.BackendConfigSuccess),
                userIdentifierState = TextFieldState(),
                serverConfig = ServerConfig.DEFAULT.copy(isOnPremises = false),
                onNextClicked = {},
                onBackendConfigLinkEntered = {},
                onBackendConfigSuccessContinue = {},
                canNavigateBack = false,
                navigateBack = {},
            )
        }
    }
}
