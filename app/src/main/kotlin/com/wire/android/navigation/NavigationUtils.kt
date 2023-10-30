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

import android.annotation.SuppressLint
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptionsBuilder
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.utils.navGraph
import com.ramcosta.composedestinations.utils.route
import com.wire.android.appLogger
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.destinations.Destination
import com.wire.kalium.logger.obfuscateId

@SuppressLint("RestrictedApi")
internal fun NavController.navigateToItem(command: NavigationCommand) {

    fun firstDestination() = currentBackStack.value.firstOrNull { it.route() is DestinationSpec<*> }
    fun lastDestination() = currentBackStack.value.lastOrNull { it.route() is DestinationSpec<*> }
    fun lastNestedGraph() = lastDestination()?.takeIf { it.navGraph() != navGraph }?.navGraph()
    fun firstDestinationWithRoute(route: String) =
        currentBackStack.value.firstOrNull { it.destination.route?.getBaseRoute() == route.getBaseRoute() }
    fun lastDestinationFromOtherGraph(graph: NavGraphSpec) = currentBackStack.value.lastOrNull { it.navGraph() != graph }

    appLogger.d("[$TAG] -> command: ${command.destination.route.obfuscateId()}")
    navigate(command.destination) {
        when (command.backStackMode) {
            BackStackMode.CLEAR_WHOLE, BackStackMode.CLEAR_TILL_START -> {
                val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                popUpTo(inclusive) { firstDestination() }
            }

            BackStackMode.REMOVE_CURRENT -> {
                popUpTo(true) { lastDestination() }
            }

            BackStackMode.REMOVE_CURRENT_NESTED_GRAPH -> {
                popUpTo(
                    getInclusive = { it.route() is NavGraphSpec },
                    getNavBackStackEntry = { lastNestedGraph()?.let { lastDestinationFromOtherGraph(it) } }
                )
            }

            BackStackMode.REMOVE_CURRENT_AND_REPLACE -> {
                popUpTo(true) { lastDestination() }
                popUpTo(true) { firstDestinationWithRoute(command.destination.route) }
            }

            BackStackMode.UPDATE_EXISTED -> {
                popUpTo(true) { firstDestinationWithRoute(command.destination.route) }
            }

            BackStackMode.NONE -> {
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavOptionsBuilder.popUpTo(
    inclusive: Boolean,
    getNavBackStackEntry: () -> NavBackStackEntry?,
) = popUpTo({ inclusive }, getNavBackStackEntry)

private fun NavOptionsBuilder.popUpTo(
    getInclusive: (NavBackStackEntry) -> Boolean,
    getNavBackStackEntry: () -> NavBackStackEntry?,
) {
    getNavBackStackEntry()?.let { entry ->
        popUpTo(entry.destination.id) {
            this.inclusive = getInclusive(entry)
        }
    }
}

internal fun NavDestination.toDestination(): Destination? =
    this.route?.let { currentRoute -> NavGraphs.root.destinationsByRoute[currentRoute] }

fun String.getBaseRoute(): String =
    this.indexOfAny(listOf("?", "/")).let {
        if (it != -1) this.substring(0, it)
        else this
    }

private const val TAG = "NavigationUtils"
