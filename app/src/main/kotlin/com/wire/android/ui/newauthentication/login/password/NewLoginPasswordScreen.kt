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

package com.wire.android.ui.newauthentication.login.password

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.authentication.ServerTitle
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.newauthentication.login.NewLoginContainer
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.authentication.login.email.LoginEmailVerificationCodeScreen
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@RootNavGraph
@WireDestination(
    navArgsDelegate = LoginNavArgs::class,
    style = AuthSlideNavigationAnimation::class,
)
@Composable
fun NewLoginPasswordScreen(
    navigator: Navigator,
    loginEmailViewModel: LoginEmailViewModel = hiltViewModel()
) {
    NewLoginContainer(
        title = stringResource(id = R.string.enterprise_login_title),
        canNavigateBack = true,
        onNavigateBack = navigator::navigateBack
    ) {
        LoginContent(
            onSuccess = { initialSyncCompleted, isE2EIRequired ->
                val destination = if (isE2EIRequired) E2EIEnrollmentScreenDestination
                else if (initialSyncCompleted) HomeScreenDestination
                else InitialSyncScreenDestination

                navigator.navigate(NavigationCommand(destination, BackStackMode.CLEAR_WHOLE))
            },
            onRemoveDeviceNeeded = {
                navigator.navigate(NavigationCommand(RemoveDeviceScreenDestination, BackStackMode.CLEAR_WHOLE))
            },
            loginEmailViewModel = loginEmailViewModel,
        )
    }
}

@Composable
private fun LoginContent(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        /*
         TODO: we can change it to be a nested navigation graph when Compose Destinations 2.0 is released,
               right now it's not possible to make start destination for nested graph with mandatory arguments.
               More on that here: https://github.com/raamcosta/compose-destinations/issues/185
         */
        AnimatedContent(
            targetState = loginEmailViewModel.secondFactorVerificationCodeState.isCodeInputNecessary,
            transitionSpec = { TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition) }
        ) { isCodeInputNecessary ->
            if (isCodeInputNecessary) {
                LoginEmailVerificationCodeScreen(loginEmailViewModel)
            } else {
                MainLoginContent(onSuccess, onRemoveDeviceNeeded, loginEmailViewModel)
            }
        }
    }
}

@Composable
private fun MainLoginContent(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
) {
    val ssoDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    FeatureDisabledWithProxyDialogContent(dialogState = ssoDisabledWithProxyDialogState)

    if (loginEmailViewModel.serverConfig.isOnPremises) {
        ServerTitle(
            serverLinks = loginEmailViewModel.serverConfig,
            style = MaterialTheme.wireTypography.body01
        )
        VerticalSpace.x8()
    }
    LoginEmailScreen(
        onSuccess = onSuccess,
        onRemoveDeviceNeeded = onRemoveDeviceNeeded,
        loginEmailViewModel = loginEmailViewModel,
        fillMaxHeight = false,
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewNewLoginPasswordScreen() = WireTheme {
    WireTheme {
        MainLoginContent(
            onSuccess = { _, _ -> },
            onRemoveDeviceNeeded = {},
            loginEmailViewModel = hiltViewModel(),
        )
    }
}
