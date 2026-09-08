/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.navigation

/**
 * Applies Wire navigation semantics without depending on Android, Compose, or a navigation host.
 *
 * Navigation 3 can apply the returned list directly to its application-owned back stack. Keeping
 * this operation pure makes existing back-stack behavior independently testable.
 */
fun reduceBackStack(
    backStack: List<WireRoute>,
    command: WireNavigationCommand,
): List<WireRoute> {
    val reduced = backStack.toMutableList()

    when (command.backStackMode) {
        WireBackStackMode.CLEAR_TILL_START -> {
            if (reduced.size > 1) {
                reduced.subList(1, reduced.size).clear()
            }
        }

        WireBackStackMode.CLEAR_WHOLE -> reduced.clear()
        WireBackStackMode.REMOVE_CURRENT -> reduced.removeLastOrNull()
        WireBackStackMode.REMOVE_CURRENT_NESTED_FLOW -> reduced.removeCurrentFlow()
        WireBackStackMode.UPDATE_EXISTING -> reduced.removeFromFirst(command.destination.routeId)
        WireBackStackMode.REMOVE_CURRENT_AND_REPLACE -> {
            reduced.removeLastOrNull()
            reduced.removeFromFirst(command.destination.routeId)
        }

        WireBackStackMode.POP_CONSECUTIVE_SAME_ROUTES -> reduced.removeConsecutiveRoutesFromTop()
        WireBackStackMode.NONE -> Unit
    }

    reduced.addOrReplaceSingleTop(command)
    return reduced
}

private fun MutableList<WireRoute>.removeCurrentFlow() {
    val currentFlowId = lastOrNull()?.flowId ?: return
    while (lastOrNull()?.flowId == currentFlowId) {
        removeLast()
    }
}

private fun MutableList<WireRoute>.removeFromFirst(routeId: String) {
    val existingIndex = indexOfFirst { it.routeId == routeId }
    if (existingIndex >= 0) {
        subList(existingIndex, size).clear()
    }
}

private fun MutableList<WireRoute>.removeConsecutiveRoutesFromTop() {
    val currentRouteId = lastOrNull()?.routeId ?: return
    while (lastOrNull()?.routeId == currentRouteId) {
        removeLast()
    }
}

private fun MutableList<WireRoute>.addOrReplaceSingleTop(command: WireNavigationCommand) {
    if (command.launchSingleTop && lastOrNull()?.routeId == command.destination.routeId) {
        this[lastIndex] = command.destination
    } else {
        add(command.destination)
    }
}
