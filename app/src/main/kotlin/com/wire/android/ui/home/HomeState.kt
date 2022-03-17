@file:OptIn(ExperimentalAnimationApi::class)

package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.navigation.HomeNavigationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeState(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState,
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    val currentNavigationItem: HomeNavigationItem
) {

    var scrollPosition by mutableStateOf(0)
        private set

    var homeBottomSheetContent by mutableStateOf(bottomSheetContent)
        private set

    fun expandBottomSheet() {
        coroutineScope.launch { bottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }

    fun changeBottomSheetContent(content: @Composable ColumnScope.() -> Unit) {
        homeBottomSheetContent = content
    }

    fun updateScrollPosition(newScrollPosition: Int) {
        scrollPosition = newScrollPosition
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
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun rememberHomeState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)? = null
): HomeState {
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val navigationItem = when (navBackStackEntry?.destination?.route) {
        HomeNavigationItem.Archive.route -> HomeNavigationItem.Archive
        HomeNavigationItem.Vault.route -> HomeNavigationItem.Vault
        else -> HomeNavigationItem.Conversations
    }

    val homeState = remember(
        navigationItem
    ) {
        HomeState(
            coroutineScope,
            navController,
            drawerState,
            bottomSheetState,
            bottomSheetContent,
            navigationItem
        )
    }

    return homeState
}
