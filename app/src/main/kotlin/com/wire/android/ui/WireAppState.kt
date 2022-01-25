package com.wire.android.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.main.navigation.MainNavigationScreenItem
import com.wire.android.ui.main.navigation.getCurrentNavigationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WireAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {

    val drawerState
        get() = scaffoldState.drawerState

    private val currentNavigationItem
        @Composable get() = navController.getCurrentNavigationItem()

    val screenTitle
        @Composable get() = stringResource(currentNavigationItem?.title ?: R.string.app_name)

    fun navigateToItem(item: MainNavigationScreenItem) {
        navController.navigate(item.route) {
            navController.graph.startDestinationRoute?.let { route ->
                popUpTo(route) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
        coroutineScope.launch { drawerState.close() }
    }

}

@Composable
fun rememberWireAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState, navController, coroutineScope) {
    WireAppState(scaffoldState, navController, coroutineScope)
}

