package com.wire.android.ui.main.navigation

import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
internal fun navigateToItem(
    navController: NavController,
    item: MainNavigationScreenItem,
    scope: CoroutineScope,
    drawerState: DrawerState,
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
    scope.launch { drawerState.close() }
}

@Composable
internal fun NavController.getCurrentNavigationItem(): MainNavigationScreenItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return MainNavigationScreenItem.fromRoute(currentRoute)
}

@Composable
internal fun NavController.isCurrentNavigationItemSearchable(): Boolean = getCurrentNavigationItem()?.hasSearchableTopBar ?: false
