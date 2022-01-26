package com.wire.android.ui.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.drawer.WireDrawer
import com.wire.android.ui.main.navigation.MainNavigationScreenItem
import com.wire.android.ui.main.navigation.MainTopBar
import com.wire.android.ui.main.navigation.isCurrentNavigationItemSearchable

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val topBar: @Composable () -> Unit = {
        MainTopBar(
            scope = scope,
            drawerState = drawerState,
            navController = navController,
            hasSearchBar = navController.isCurrentNavigationItemSearchable()
        )
    }
    val drawerContent: @Composable (ColumnScope.() -> Unit) = {
//        WireDrawer()
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
            MainNavigationGraph(navController = navController)
        }
    }
}

@Composable
fun MainNavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = MainNavigationScreenItem.Conversations.route) {
        MainNavigationScreenItem.values().forEach { item ->
            composable(route = item.route, content = item.content)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
