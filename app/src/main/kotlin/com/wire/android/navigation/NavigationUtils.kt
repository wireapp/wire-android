package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.appLogger

@ExperimentalMaterial3Api
internal fun NavController.navigateToItem(command: NavigationCommand) {
    currentBackStackEntry?.savedStateHandle?.remove<Map<String, Any>>(EXTRA_BACK_NAVIGATION_ARGUMENTS)
    navigate(command.destination) {
        when (command.backStackMode) {
            BackStackMode.CLEAR_WHOLE, BackStackMode.CLEAR_TILL_START -> {
                backQueue.firstOrNull { it.destination.route != null }?.let { entry ->
                    val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                    val startId = entry.destination.id
                    popBackStack(startId, inclusive)
                }
            }
            BackStackMode.REMOVE_CURRENT -> {
                run {
                    backQueue.lastOrNull { it.destination.route != null }?.let { entry ->
                        val inclusive = true
                        val startId = entry.destination.id
                        popBackStack(startId, inclusive)
                    }
                }
            }
            BackStackMode.NONE -> {}
        }
        launchSingleTop = true
        restoreState = true
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
            it.savedStateHandle[EXTRA_BACK_NAVIGATION_ARGUMENTS] = arguments
        }
    }
    return popBackStack()
}

internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val currentRoute = this.currentDestination?.route
    return NavigationItem.fromRoute(currentRoute)
}
