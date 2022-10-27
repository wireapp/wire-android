package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.wire.android.appLogger

@ExperimentalMaterial3Api
internal fun NavController.navigateToItem(command: NavigationCommand) {
    currentBackStackEntry?.savedStateHandle?.remove<Map<String, Any>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)
    navigate(command.destination) {
        when (command.backStackMode) {
            BackStackMode.CLEAR_WHOLE, BackStackMode.CLEAR_TILL_START -> {
                val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                popBackStack(inclusive) { backQueue.firstOrNull { it.destination.route != null } }
            }
            BackStackMode.REMOVE_CURRENT -> {
                popBackStack(true) { backQueue.lastOrNull { it.destination.route != null } }
            }
            BackStackMode.UPDATE_EXISTED -> {
                NavigationItem.fromRoute(command.destination)?.let { navItem ->
                    popBackStack(true) { backQueue.firstOrNull { it.destination.route == navItem.getCanonicalRoute() } }
                }
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

/**
 * @return true if the stack was popped at least once and the user has been navigated to another destination,
 * false otherwise
 */
internal fun NavController.popWithArguments(arguments: Map<String, Any>?): Boolean {
    previousBackStackEntry?.let {
        it.savedStateHandle.remove<Map<String, Any>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)
        arguments?.let { arguments ->
            appLogger.d("Destination is ${it.destination}")
            it.savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] = arguments.toMap()
        }
    }
    return popBackStack()
}

internal fun NavController.getCurrentNavigationItem(): NavigationItem? =
    this.currentDestination?.route?.let { currentRoute ->
        NavigationItem.fromRoute(currentRoute)
    }
