package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@ExperimentalMaterial3Api
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

                    if (command.backStackMode == BackStackMode.CLEAR_CURRENT)
                        popBackStack()
                    else
                        popBackStack(startId, inclusive)
                }
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

@ExperimentalMaterial3Api
@Composable
internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return NavigationItem.fromRoute(currentRoute)
}
