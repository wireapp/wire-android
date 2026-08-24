/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login.code

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.common.textfield.clearAutofillTree

/** Host adapter: the feature renders content while this layer owns mutation and back-stack policy. */
@Composable
internal fun NewLoginVerificationCodeRouteScreen(
    loginEmailViewModel: AppLoginEmailViewModel,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
) {
    clearAutofillTree()
    LaunchedEffect(loginEmailViewModel) {
        // The new-login flow explicitly requires confirmation; a complete code only enables Next.
        loginEmailViewModel.autoLoginWhenFullCodeEntered = false
    }
    val navigateBack = {
        loginEmailViewModel.onCodeVerificationBackPress()
        onNavigateBack()
    }
    BackHandler(onBack = navigateBack)

    NewLoginVerificationCodeContent(
        codeTextState = loginEmailViewModel.secondFactorVerificationCodeTextState,
        codeState = loginEmailViewModel.secondFactorVerificationCodeState,
        isLoading = loginEmailViewModel.loginState.flowState is LoginState.Loading,
        onResendCode = loginEmailViewModel::onCodeResend,
        onLoginButtonClick = loginEmailViewModel::login,
        canNavigateBack = canNavigateBack,
        navigateBack = navigateBack,
    )
}
