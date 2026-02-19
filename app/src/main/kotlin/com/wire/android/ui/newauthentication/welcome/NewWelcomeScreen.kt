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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthNoNavigationAnimation
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.WelcomeScreenDestination

@WireRootDestination(start = true)
@Composable
fun WelcomeChooserScreen(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector,
) {
    LaunchedEffect(Unit) {
        val destination = if (loginTypeSelector.canUseNewLogin()) NewLoginScreenDestination() else WelcomeScreenDestination()
        navigator.navigate(NavigationCommand(destination))
    }
}

// this is completely empty initial screen that allows to show just BackgroundType.Auth until any potential deep link is handled
@WireRootDestination(
    style = AuthNoNavigationAnimation::class,
)
@Composable
fun NewWelcomeEmptyStartScreen() {
    Box(modifier = Modifier.fillMaxSize()) // empty Box to keep proper bounds of the screen for transition animation to the next screen
}
