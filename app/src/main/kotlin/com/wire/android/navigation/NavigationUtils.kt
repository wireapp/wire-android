package com.wire.android.navigation

import androidx.navigation.NavController

internal fun navigateToItem(
    navController: NavController,
    command: NavigationCommand
) {
    navController.navigate(command.destination) {
        if (command.backStackMode.shouldClear()) {
            navController.run {
                backQueue.firstOrNull { it.destination.route != null }?.let { entry ->
                    val inclusive = command.backStackMode == BackStackMode.CLEAR_WHOLE
                    val startId = entry.destination.id

                    popBackStack(startId, inclusive)
                }
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val currentRoute = this.currentDestination?.route
    return NavigationItem.fromRoute(currentRoute)
}
