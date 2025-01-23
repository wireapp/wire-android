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

package com.wire.android.ui.newauthentication.welcome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.BuildConfig
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.authentication.welcome.WelcomeScreenState
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.NewLoginScreenDestination
import com.wire.android.ui.destinations.NewWelcomeScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@RootNavGraph(start = true)
@WireDestination(
    navArgsDelegate = LoginNavArgs::class
)
@Composable
// this is a temporary solution because annotation argument "start" must be a compile-time constant
// TODO: remove this composable as well when removing old WelcomeScreen and set start = true for NewWelcomeScreen
fun WelcomeChooserScreen(navigator: Navigator) {
    LaunchedEffect(Unit) {
        val destination = if (BuildConfig.ENTERPRISE_LOGIN_ENABLED) NewWelcomeScreenDestination else WelcomeScreenDestination
        navigator.navigate(NavigationCommand(destination))
    }
}

@RootNavGraph(start = false)
@WireDestination(
    style = AuthPopUpNavigationAnimation::class,
)
@Composable
fun NewWelcomeScreen(
    navigator: Navigator,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    WelcomeContent(
        viewModel.state,
        navigator::navigateBack,
        navigator::navigate
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeScreenState,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit
) {
    val maxAccountsReachedDialogState = rememberVisibilityState<MaxAccountsReachedDialogState>()
    MaxAccountsReachedDialog(dialogState = maxAccountsReachedDialogState) { navigateBack() }
    if (state.maxAccountsReached) {
        maxAccountsReachedDialogState.show(maxAccountsReachedDialogState.savedState ?: MaxAccountsReachedDialogState)
    }

    Box(modifier = Modifier.fillMaxSize()) // empty Box to keep proper bounds of the screen for transition animation to the next screen

    LaunchedEffect(Unit) {
        if (state.maxAccountsReached.not()) {
            delay(1.seconds) // small delay to resolve the navigation
            navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewNewWelcomeScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout()
        WelcomeContent(
            state = WelcomeScreenState(ServerConfig.DEFAULT),
            navigateBack = {},
            navigate = {}
        )
    }
}
