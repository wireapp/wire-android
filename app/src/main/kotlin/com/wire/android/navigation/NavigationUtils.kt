package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import com.wire.android.appLogger

@ExperimentalMaterial3Api
internal fun navigateToItem(
    navController: NavController,
    command: NavigationCommand
) {
    navController.navigate(command.destination) {
        when (command.backStackMode) {
            BackStackMode.CLEAR_WHOLE, BackStackMode.CLEAR_TILL_START -> {
                navController.run {
                    backQueue.firstOrNull { it.destination.route != null }?.let { entry ->
                        val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                        val startId = entry.destination.id
                        popBackStack(startId, inclusive)
                    }
                }
            }
            BackStackMode.REMOVE_CURRENT -> {
                navController.run {
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
        arguments?.forEach { (key, value) ->
            appLogger.d("Destination is ${it.destination}")
            it.savedStateHandle[key] = value
        }
    }
    return popBackStack()
}

internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val currentRoute = this.currentDestination?.route
    return NavigationItem.fromRoute(currentRoute)
}
