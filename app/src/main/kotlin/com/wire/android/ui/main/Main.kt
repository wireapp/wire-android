package com.wire.android.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DrawerValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.main.navigation.MainDrawer
import com.wire.android.ui.main.navigation.MainNavigationScreenItem
import com.wire.android.ui.main.navigation.MainTopBar
import com.wire.android.ui.main.navigation.isCurrentNavigationItemSearchable

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainScreen() {
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val topBar: @Composable () -> Unit = {
        MainTopBar(
            scope = scope,
            scaffoldState = scaffoldState,
            navController = navController,
            hasSearchBar = navController.isCurrentNavigationItemSearchable()
        )
    }
    val drawerContent: @Composable (ColumnScope.() -> Unit) = {
        MainDrawer(scope = scope, scaffoldState = scaffoldState, navController = navController)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = topBar,
        drawerContent = drawerContent,
    ) {
        MainNavigationGraph(navController = navController)
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
