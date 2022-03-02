package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val homeState = rememberHomeState()

    val topBar: @Composable () -> Unit = {
        HomeTopBar(
            homeState.itemTitle,
            homeState.isItemSearchable,
            homeState.scrollPosition,
            { homeState.coroutineScope.launch { viewModel.navigateToUserProfile() } },
            { homeState.coroutineScope.launch { homeState.drawerState.open() } })
    }

    val drawerContent: @Composable ColumnScope.() -> Unit = {
        HomeDrawer(
            homeState.drawerState,
            homeState.itemRoute,
            homeState.navController,
            HomeNavigationItem.all,
            homeState.coroutineScope,
            viewModel
        )
    }

    NavigationDrawer(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        drawerState = homeState.drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = homeState.isItemSwipeable
    ) {

        val homeContent: @Composable () -> Unit = {
            Box {
                val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route
                HomeNavigationGraph(
                    homeState = homeState,
                    navController = homeState.navController,
                    startDestination = startDestination
                )
                // We are not including the topBar in the Scaffold to correctly handle the collapse scroll effect on the search,
                // which will not be possible when using Scaffold topBar argument
                topBar()
            }
        }

        val homeScreen: @Composable () -> Unit = homeState.homeBottomSheetContent?.run {
            {
                WireModalSheetLayout(
                    sheetState = homeState.bottomSheetState,
                    sheetContent = this
                ) {
                    homeContent()
                }
            }
        } ?: { homeContent() }

        homeScreen()
    }
    BackHandler(enabled = homeState.drawerState.isOpen) { homeState.coroutineScope.launch { homeState.drawerState.close() } }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
class HomeState(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val bottomSheetState: ModalBottomSheetState,
    bottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    val itemTitle: Int,
    val itemRoute: String,
    val isItemSearchable: Boolean,
    val isItemSwipeable: Boolean
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

    val itemTitle by remember(navigationItem) {
        derivedStateOf { navigationItem.title }
    }

    val isItemSearchable by remember(navigationItem) {
        derivedStateOf { navigationItem.isSearchable }
    }

    val itemRoute by remember(navigationItem) {
        derivedStateOf { navigationItem.route }
    }

    val isItemSwipeable by remember(navigationItem) {
        derivedStateOf { navigationItem.isSwipeable }
    }

    val homeState = remember(
        itemTitle,
        isItemSearchable,
        itemRoute,
        isItemSwipeable
    ) {
        HomeState(
            coroutineScope,
            navController,
            drawerState,
            bottomSheetState,
            bottomSheetContent,
            itemTitle,
            itemRoute,
            isItemSearchable,
            isItemSwipeable
        )
    }

    return homeState
}
