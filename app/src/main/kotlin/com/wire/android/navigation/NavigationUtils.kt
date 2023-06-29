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
 *
 *
 */

package com.wire.android.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.ramcosta.composedestinations.navigation.navigate
import com.wire.android.appLogger
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.destinations.Destination
import com.wire.kalium.logger.obfuscateId

internal fun NavController.navigateToItem(command: NavigationCommand) {
    appLogger.d("[$TAG] -> command: ${command.destination.route.obfuscateId()}")
    navigate(command.destination) {
        when (command.backStackMode) {
            BackStackMode.CLEAR_WHOLE, BackStackMode.CLEAR_TILL_START -> {
                val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                popBackStack(inclusive) { backQueue.firstOrNull { it.destination.route != null } }
            }

            BackStackMode.REMOVE_CURRENT -> {
                popBackStack(true) { backQueue.lastOrNull { it.destination.route != null } }
            }

            BackStackMode.REMOVE_CURRENT_AND_REPLACE -> {
                popBackStack(true) { backQueue.lastOrNull { it.destination.route != null } }
                popBackStack(true) { backQueue.firstOrNull { it.destination.route == command.destination.route } }
            }

            BackStackMode.UPDATE_EXISTED -> {
                popBackStack(true) { backQueue.firstOrNull { it.destination.route == command.destination.route } }
            }

            BackStackMode.NONE -> {
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavController.popBackStack(
    inclusive: Boolean,
    getNavBackStackEntry: () -> NavBackStackEntry?,
) {
    getNavBackStackEntry()?.let { entry ->
        val startId = entry.destination.id
        popBackStack(startId, inclusive)
    }
}

internal fun NavDestination.toDestination(): Destination? =
    this.route?.let { currentRoute -> NavGraphs.root.destinationsByRoute[currentRoute] }

fun String.getPrimaryRoute(): String {
    val splitByQuestion = this.split("?")
    val splitBySlash = this.split("/")

    val primaryRoute = when {
        splitByQuestion.size > 1 -> splitByQuestion[0]
        splitBySlash.size > 1 -> splitBySlash[0]
        else -> this
    }
    return primaryRoute
}

private const val TAG = "NavigationUtils"
