package com.wire.android.ui.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreenIndex: Int?, viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val currentItem = HomeNavigationItem.getCurrentNavigationItem(navController)
    val drawerItems = listOf(HomeNavigationItem.Conversations, HomeNavigationItem.Archive, HomeNavigationItem.Vault)

    val topBar: @Composable () -> Unit = {
        HomeTopBar(currentItem.title, currentItem.isSearchable, drawerState, viewModel)
    }
    val drawerContent: @Composable ColumnScope.() -> Unit = {
        HomeDrawer(drawerState, currentItem.route, navController, drawerItems, viewModel)
    }

    NavigationDrawer(
        drawerContainerColor = Color.White,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        drawerState = drawerState,
        drawerContent = drawerContent
    ) {
        Scaffold(
            topBar = topBar,
        ) {
            val startDestination = drawerItems.getOrNull(startScreenIndex ?: 0)?.route
            HomeNavigationGraph(navController = navController, startDestination)
        }
    }
}
