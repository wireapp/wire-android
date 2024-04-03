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
package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.wire.android.ui.NavGraphs

class Navigator(val finish: () -> Unit, val navController: NavHostController) {
    private val isResumed: Boolean
        get() = navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

    /**
     * Navigates to the specified screen.
     * @param navigationCommand command containing the destination and back stack mode
     * @param onlyIfResumed if true, will ignore the navigation action if the current `NavBackStackEntry`
     * is not in the RESUMED state. This avoids duplicate navigation actions and should be used when it's the user action
     * or when we simply don't want to make more than one navigation action at a time (skip some destinations instantly).
     * More here: https://composedestinations.rafaelcosta.xyz/navigation/basics#avoiding-duplicate-navigation
     */
    fun navigate(navigationCommand: NavigationCommand, onlyIfResumed: Boolean = false) {
        if (onlyIfResumed && !isResumed) return
        navController.navigateToItem(navigationCommand)
    }

    /**
     * Navigates back to the previous screen.
     * @param onlyIfResumed if true, will ignore the navigation action if the current `NavBackStackEntry`
     * is not in the RESUMED state. This avoids duplicate navigation actions and should be used when it's the user action
     * or when we simply don't want to make more than one navigation action at a time (skip some destinations instantly).
     * More here: https://composedestinations.rafaelcosta.xyz/navigation/basics#avoiding-duplicate-navigation
     */
    fun navigateBack(onlyIfResumed: Boolean = false) {
        if (onlyIfResumed && !isResumed) return
        if (!navController.popBackStack()) finish()
    }
}

@Composable
fun rememberNavigator(finish: () -> Unit): Navigator {
    val navController = rememberTrackingAnimatedNavController {
        NavGraphs.root.destinationsByRoute[it]?.let { it::class.simpleName } // there is a proguard rule for Routes
    }
    return remember(finish, navController) { Navigator(finish, navController) }
}

val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator provided") }
