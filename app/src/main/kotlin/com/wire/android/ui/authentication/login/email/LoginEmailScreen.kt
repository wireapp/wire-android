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

package com.wire.android.ui.authentication.login.email

import com.wire.android.ui.authentication.login.AppLoginDialogError
import com.wire.android.ui.authentication.login.AppLoginState
import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.R as commonR
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.login.toLoginDialogErrorData
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.EmailAlreadyInUseClaimedDomainDialog
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.focusedBorder
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginEmailScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginEmailViewModel: AppLoginEmailViewModel,
    scrollState: ScrollState = rememberScrollState(),
    fillMaxHeight: Boolean = true,
) {
    val context = LocalContext.current
    clearAutofillTree()

    com.wire.android.ui.authentication.login.email.LoginEmailContent(
        scrollState = scrollState,
        state = com.wire.android.ui.authentication.login.email.LoginEmailPresentationState(
            loading = loginEmailViewModel.loginState.flowState is LoginState.Loading,
            loginEnabled = loginEmailViewModel.loginState.loginEnabled,
            userIdentifierEnabled = loginEmailViewModel.loginState.userIdentifierEnabled,
            invalidUserIdentifier = loginEmailViewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue,
            invalidProxyIdentifier = loginEmailViewModel.loginState.flowState is LoginState.Error.TextFieldError.InvalidValue,
            showInvalidCredentialsError = loginEmailViewModel.loginState.showInvalidCredentialsError,
            proxyAuthRequired = loginEmailViewModel.serverConfig.isProxyAuthRequired,
            apiProxyUrl = loginEmailViewModel.serverConfig.apiProxy?.host,
        ),
        text = com.wire.android.ui.authentication.login.email.LoginEmailText(
            wireCredentials = stringResource(R.string.label_wire_credentials),
            userIdentifierLabel = stringResource(R.string.login_user_identifier_label),
            userIdentifierDescription = stringResource(R.string.content_description_login_user_identifier_field),
            invalidUserIdentifier = stringResource(R.string.login_error_invalid_user_identifier),
            passwordDescription = stringResource(R.string.content_description_login_password_field),
            invalidCredentials = stringResource(R.string.login_error_invalid_credentials_message),
            forgotPassword = stringResource(R.string.login_forgot_password),
            openLinkDescription = stringResource(commonR.string.content_description_open_link_label),
            proxyCredentials = stringResource(R.string.label_proxy_credentials),
            proxyDescription = { host -> context.getString(R.string.proxy_credential_description, host) },
            login = stringResource(R.string.label_login),
            loggingIn = stringResource(R.string.label_logging_in),
        ),
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        proxyIdentifierTextState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordTextState = loginEmailViewModel.proxyPasswordTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        onLoginButtonClick = loginEmailViewModel::login,
        onForgotPasswordClick = { openForgotPasswordPage(context, loginEmailViewModel.serverConfig.forgotPassword) },
        fillMaxHeight = fillMaxHeight,
    )

    LoginEmailStateNavigationAndDialogs(
        state = loginEmailViewModel.loginState.flowState,
        domainClaimedByOrg = loginEmailViewModel.domainClaimedByOrg,
        onClearLoginErrors = loginEmailViewModel::clearLoginErrors,
        onSuccess = onSuccess,
        onRemoveDeviceNeeded = onRemoveDeviceNeeded,
    )
}

@Composable
private fun LoginEmailStateNavigationAndDialogs(
    state: AppLoginState,
    domainClaimedByOrg: DomainClaimedByOrg?,
    onClearLoginErrors: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
) {
    val emailAlreadyInUseClaimedDomainDialogState = rememberVisibilityState<DomainClaimedByOrg.Claimed>()
    val handleLoginStateNavigation: (AppLoginState) -> Unit = {
        when (it) {
            is LoginState.Success<*> -> onSuccess(it.initialSyncCompleted, it.isE2EIRequired, it.userId as UserId)
            is LoginState.Error.TooManyDevicesError<*> -> {
                onClearLoginErrors()
                onRemoveDeviceNeeded(it.userId as UserId)
            }
            else -> {
                /* do nothing */
            }
        }
    }

    LaunchedEffect(state) {
        val isStateCompleted = state is LoginState.Success<*> || state is LoginState.Error.TooManyDevicesError<*>
        if (isStateCompleted && domainClaimedByOrg is DomainClaimedByOrg.Claimed) {
            emailAlreadyInUseClaimedDomainDialogState.show(domainClaimedByOrg)
        } else {
            handleLoginStateNavigation(state)
        }
    }

    if (state is LoginState.Error.DialogError<*, *>) {
        LoginErrorDialog((state as AppLoginDialogError).toLoginDialogErrorData(), onClearLoginErrors)
    }
    EmailAlreadyInUseClaimedDomainDialog(
        dialogState = emailAlreadyInUseClaimedDomainDialogState,
        onDismiss = {
            emailAlreadyInUseClaimedDomainDialogState.dismiss()
            handleLoginStateNavigation(state)
        }
    )
}

@Composable
fun ForgotPasswordLabel(
    forgotPasswordUrl: String,
    modifier: Modifier = Modifier,
    textColor: Color = colorsScheme().primary,
) {
    val context = LocalContext.current
    com.wire.android.ui.authentication.login.email.ForgotPasswordLink(
        onClick = { openForgotPasswordPage(context, forgotPasswordUrl) },
        modifier = modifier,
        textColor = textColor,
    )
}

private fun openForgotPasswordPage(context: Context, forgotPasswordUrl: String) {
    CustomTabsHelper.launchUrl(context, forgotPasswordUrl).also {
        appLogger.d(forgotPasswordUrl)
    }
}
