package com.wire.android.ui.main.navigation

import androidx.compose.material.ScaffoldState
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun itemClickActions(
    navController: NavController,
    item: MainNavigationScreenItem,
    scope: CoroutineScope,
    scaffoldState: ScaffoldState
) {
    navController.navigate(item.route) {
        navController.graph.startDestinationRoute?.let { route ->
            popUpTo(route) {
                saveState = true
            }
        }
        launchSingleTop = true
        restoreState = true
    }

    scope.launch { scaffoldState.drawerState.close() }
}
