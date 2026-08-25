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

package com.wire.android.ui.authentication.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.email.LoginEmailVerificationCodeScreen
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.user.UserId

/**
 * Navigation-neutral adapter used by the Navigation 3 host.
 */
@Composable
internal fun LoginRouteScreen(
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
) {
    LoginContent(
        onBackPressed = onBackPressed,
        onSuccess = onSuccess,
        onRemoveDeviceNeeded = onRemoveDeviceNeeded,
        loginNavArgs = loginNavArgs,
        loginEmailViewModel = loginEmailViewModel,
        ssoLoginResult = loginNavArgs.ssoLoginResult,
        ssoCodeAutoLogin = loginNavArgs.ssoCodeAutoLogin
    )
}

@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean, userId: UserId) -> Unit,
    onRemoveDeviceNeeded: (UserId) -> Unit,
    loginNavArgs: LoginNavArgs,
    loginEmailViewModel: AppLoginEmailViewModel,
    ssoLoginResult: DeepLinkResult.SSOLogin?,
    ssoCodeAutoLogin: SSOCodeAutoLogin?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = loginSurface(loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary),
            transitionSpec = {
                TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
            }
        ) { surface ->
            if (surface == LoginSurface.Verification) {
                LoginEmailVerificationCodeScreen(loginEmailViewModel)
            } else {
                MainLoginContent(
                    onBackPressed = onBackPressed,
                    onSuccess = onSuccess,
                    onRemoveDeviceNeeded = onRemoveDeviceNeeded,
                    loginNavArgs = loginNavArgs,
                    loginEmailViewModel = loginEmailViewModel,
                    ssoLoginResult = ssoLoginResult,
                    ssoCodeAutoLogin = ssoCodeAutoLogin
                )
            }
        }
    }
}
