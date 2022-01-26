package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

internal fun navigateToItem(
    navController: NavController,
    item: NavigationItem
) {
    navController.navigate(item.route) {
        if (!item.addingToBackStack) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return NavigationItem.fromRoute(currentRoute)
}
