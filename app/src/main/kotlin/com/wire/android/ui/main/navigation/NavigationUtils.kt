package com.wire.android.ui.main.navigation

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun navigateToItem(
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

@Composable
internal fun NavController.getCurrentNavigationItem(): MainNavigationScreenItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return MainNavigationScreenItem.fromRoute(currentRoute)
}

@Composable
internal fun NavController.isCurrentNavigationItemSearchable(): Boolean = getCurrentNavigationItem()?.hasSearchableTopBar ?: false
