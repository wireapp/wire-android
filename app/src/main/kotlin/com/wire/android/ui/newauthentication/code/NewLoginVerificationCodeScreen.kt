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
package com.wire.android.ui.newauthentication.login.code

import com.wire.android.navigation.annotation.app.WireNewLoginDestination
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.login.email.LoginButton
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.verificationcode.VerificationCode
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthSubtitle
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import com.wire.android.ui.newauthentication.login.password.LoginStateNavigationAndDialogs
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

// has to be navigated to after NewLoginPasswordScreen, otherwise there will be illegal state because it needs to reuse view model from it
@WireNewLoginDestination(
    navArgs = LoginNavArgs::class,
    style = AuthSlideNavigationAnimation::class,
)
@Composable
fun NewLoginVerificationCodeScreen(
    navigator: Navigator,
    loginEmailViewModel: LoginEmailViewModel, // provided in MainNavHost to reuse from NewLoginPasswordScreen, don't use hiltViewModel()
) {
    clearAutofillTree()
    LoginStateNavigationAndDialogs(loginEmailViewModel, navigator)

    LaunchedEffect(loginEmailViewModel) {
        loginEmailViewModel.autoLoginWhenFullCodeEntered = false
    }

    val navigateBack = {
        loginEmailViewModel.onCodeVerificationBackPress()
        navigator.navigateBack()
    }
    BackHandler {
        navigateBack()
    }

    LoginVerificationCodeContent(
        codeTextState = loginEmailViewModel.secondFactorVerificationCodeTextState,
        codeState = loginEmailViewModel.secondFactorVerificationCodeState,
        isLoading = loginEmailViewModel.loginState.flowState is LoginState.Loading,
        onResendCode = loginEmailViewModel::onCodeResend,
        onLoginButtonClick = loginEmailViewModel::login,
        canNavigateBack = navigator.navController.previousBackStackEntry != null, // if there is a previous screen to navigate back to
        navigateBack = navigateBack,
    )
}

@Composable
private fun LoginVerificationCodeContent(
    codeTextState: TextFieldState,
    codeState: VerificationCodeState,
    isLoading: Boolean,
    onResendCode: () -> Unit,
    onLoginButtonClick: () -> Unit,
    canNavigateBack: Boolean,
    navigateBack: () -> Unit,
) {
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    NewAuthTitle(
                        title = stringResource(R.string.enterprise_login_verification_code_title),
                        modifier = Modifier.padding(bottom = dimensions().spacing24x)
                    )
                    NewAuthSubtitle(
                        title = stringResource(R.string.second_factor_authentication_instructions_label, codeState.emailUsed),
                    )
                },
                canNavigateBack = canNavigateBack,
                onNavigateBack = navigateBack
            )
        }
    ) {
        VerificationCode(
            codeLength = codeState.codeLength,
            codeState = codeTextState,
            isLoading = isLoading,
            showLoadingProgress = false, // for new login we show progress on the "next" button
            isCurrentCodeInvalid = codeState.isCurrentCodeInvalid,
            onResendCode = onResendCode,
            modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing24x),
        )
        LoginButton(
            loading = isLoading,
            enabled = codeTextState.text.length == codeState.codeLength,
            text = stringResource(R.string.enterprise_login_next),
            loadingText = stringResource(R.string.enterprise_login_next),
            onClick = onLoginButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensions().spacing8x),
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginVerificationCodeScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            LoginVerificationCodeContent(
                codeTextState = TextFieldState(),
                codeState = VerificationCodeState(
                    codeLength = 6,
                    isCurrentCodeInvalid = false,
                    emailUsed = ""
                ),
                isLoading = false,
                onResendCode = {},
                onLoginButtonClick = {},
                canNavigateBack = true,
                navigateBack = {},
            )
        }
    }
}
