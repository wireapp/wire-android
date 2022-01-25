package com.wire.android.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.main.navigation.getCurrentNavigationItem
import kotlinx.coroutines.CoroutineScope


@Composable
fun rememberWireAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState, navController, coroutineScope) {
    WireAppState(scaffoldState, navController, coroutineScope)
}

class WireAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {

    val drawerState
        get() = scaffoldState.drawerState

    val currentNavigationItem
        @Composable get() = navController.getCurrentNavigationItem()

    val shouldShowSearchBar: Boolean
        @Composable get() = currentNavigationItem?.hasSearchableTopBar ?: false
}
