@file:OptIn(ExperimentalAnimationApi::class)

package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeUIState(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState,
    val currentNavigationItem: HomeNavigationItem
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

    fun closeBottomSheet() {
        coroutineScope.launch {
            if (bottomSheetState.isVisible) bottomSheetState.animateTo(ModalBottomSheetValue.Hidden)
        }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun rememberHomeUIState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberAnimatedNavController(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
): HomeUIState {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentNavigationItem = HomeNavigationItem.values().firstOrNull { it.route == currentRoute } ?: HomeNavigationItem.Conversations

    val homeState = remember(
        currentNavigationItem
    ) {
        HomeUIState(
            coroutineScope,
            navController,
            drawerState,
            bottomSheetState,
            currentNavigationItem
        )
    }

    return homeState
}
