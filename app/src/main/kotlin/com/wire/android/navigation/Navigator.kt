/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.wire.android.ui.NavGraphs

class Navigator(val finish: () -> Unit, val navController: NavHostController) {
    fun navigate(navigationCommand: NavigationCommand) {
        navController.navigateToItem(navigationCommand)
    }
    fun navigateBack() {
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

typealias Navigate = (NavigationCommand) -> Unit
typealias NavigateBack = () -> Unit
