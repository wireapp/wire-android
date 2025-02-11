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
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.BuildConfig
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.AuthNoNavigationAnimation
import com.wire.android.ui.destinations.NewLoginScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination

@RootNavGraph(start = true)
@WireDestination
@Composable
fun WelcomeChooserScreen(navigator: Navigator) {
    // this is a temporary solution because annotation argument "start" must be a compile-time constant
    // TODO: remove this composable as well when removing old WelcomeScreen and set start = true for NewWelcomeScreen
    LaunchedEffect(Unit) {
        val destination = if (BuildConfig.ENTERPRISE_LOGIN_ENABLED) NewLoginScreenDestination() else WelcomeScreenDestination()
        navigator.navigate(NavigationCommand(destination))
    }
}

@RootNavGraph
@WireDestination(
    style = AuthNoNavigationAnimation::class,
)
@Composable
// this is completely empty initial screen that allows to show just BackgroundType.Auth until any potential deep link is handled
fun NewWelcomeEmptyStartScreen() {
    Box(modifier = Modifier.fillMaxSize()) // empty Box to keep proper bounds of the screen for transition animation to the next screen
}
