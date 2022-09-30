@file:OptIn(ExperimentalAnimationApi::class)

package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.navigation.navigateToItemInHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeScreenState(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState,
    val currentNavigationItem: HomeNavigationItem,
    val snackBarHostState: SnackbarHostState
) {

    var homeBottomSheetContent: @Composable (ColumnScope.() -> Unit)? by mutableStateOf(null)
        private set

    var snackbarState: HomeSnackbarState by mutableStateOf(HomeSnackbarState.None) // TODO replace with Flow
        private set

    fun setSnackBarState(state: HomeSnackbarState) {
        snackbarState = state
        if (state != HomeSnackbarState.None) closeBottomSheet()
    }

    fun clearSnackbarMessage() {
        setSnackBarState(HomeSnackbarState.None)
    }

    fun openBottomSheet() {
        coroutineScope.launch {
            if (!bottomSheetState.isVisible) bottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
        }
    }

    private fun closeBottomSheet() {
        coroutineScope.launch {
            if (bottomSheetState.isVisible) bottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
        }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }

    fun closeDrawer() {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }

    fun navigateTo(homeNavigationItem: HomeNavigationItem) {
        navigateToItemInHome(navController, homeNavigationItem)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun rememberHomeScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberAnimatedNavController(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
    snackBarHostState: SnackbarHostState = remember { SnackbarHostState() }
): HomeScreenState {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentNavigationItem = HomeNavigationItem.values().firstOrNull { it.route == currentRoute } ?: HomeNavigationItem.Conversations

    val homeState = remember(
        currentNavigationItem
    ) {
        HomeScreenState(
            coroutineScope,
            navController,
            drawerState,
            bottomSheetState,
            currentNavigationItem,
            snackBarHostState
        )
    }

    return homeState
}
