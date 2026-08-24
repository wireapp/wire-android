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

/**
 * Navigation-neutral adapter used by Navigation 3.
 */
@Composable
fun WelcomeChooserScreen(
    onChooseLogin: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onChooseLogin()
    }
}

// This empty initial screen shows only BackgroundType.Auth until a potential deep link is handled.
@Composable
@Suppress("ComposeModifierMissing")
fun NewWelcomeEmptyStartScreen() {
    // Keep proper bounds for the transition animation to the next screen.
    Box(modifier = Modifier.fillMaxSize())
}
