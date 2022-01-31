package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentItem = HomeNavigationItem.getCurrentNavigationItem(navController)
    val scope = rememberCoroutineScope()

    val topBar: @Composable () -> Unit = {
        HomeTopBar(currentItem.title, currentItem.isSearchable, drawerState, scope, viewModel)
    }
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        HomeDrawer(drawerState, currentItem.route, navController, HomeNavigationItem.all, scope, viewModel)
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    NavigationDrawer(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        drawerState = drawerState,
        drawerContent = drawerContent
    ) {
        Scaffold(
            topBar = topBar,
        ) {
            val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route
            HomeNavigationGraph(navController = navController, startDestination)
        }
    }
}
