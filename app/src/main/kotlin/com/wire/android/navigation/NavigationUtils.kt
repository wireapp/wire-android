package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
            BackStackMode.NONE -> {}
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun NavController.popWithArguments(arguments: Map<String, Any>?) {
    previousBackStackEntry?.let {
        arguments?.forEach { (key, value) ->
            appLogger.d("Destination is ${it.destination}")
            it.savedStateHandle[key] = value
        }
    }
    popBackStack()
}

@ExperimentalMaterial3Api
@Composable
internal fun NavController.getCurrentNavigationItem(): NavigationItem? {
    val navBackStackEntry by currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    return NavigationItem.fromRoute(currentRoute)
}
