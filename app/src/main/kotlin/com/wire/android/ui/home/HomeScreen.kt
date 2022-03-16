package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.UserStatus
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.NavigationIconType
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.AppTopBarWithSearchBar
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar


@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    val homeState = rememberHomeState()

    with(homeState) {
        val drawerContent: @Composable ColumnScope.() -> Unit = {
            HomeDrawer(
                drawerState = drawerState,
                currentRoute = currentNavigationItem.route,
                homeNavController = navController,
                topItems = HomeNavigationItem.all,
                scope = coroutineScope,
                viewModel = viewModel
            )
        }

        NavigationDrawer(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerTonalElevation = 0.dp,
            drawerShape = RectangleShape,
            drawerState = drawerState,
            drawerContent = drawerContent,
            gesturesEnabled = drawerState.isOpen
        ) {
            HomeContent(
                scrollPosition = homeState.scrollPosition,
                homeBottomSheetContent = homeState.homeBottomSheetContent,
                homeBottomSheetState = homeState.bottomSheetState,
                homeTopBar = {
                    HomeTopBar(
                        currentNavigationItem = homeState.currentNavigationItem,
                        onOpenDrawerClicked = { openDrawer() },
                        onNavigateToUserProfile = { viewModel.navigateToUserProfile() },
                    )
                },
                currentNavigationItem = homeState.currentNavigationItem,
                homeNavigationGraph = { HomeNavigationGraph(homeState = homeState, startScreen = startScreen) }
            )
        }

        BackHandler(enabled = drawerState.isOpen) { closeDrawer() }
    }
}


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    scrollPosition: Int,
    homeBottomSheetState: ModalBottomSheetState,
    homeBottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    currentNavigationItem: HomeNavigationItem,
    homeNavigationGraph: @Composable () -> Unit,
    homeTopBar: @Composable () -> Unit
) {
    with(currentNavigationItem) {
        val homeContent = @Composable {
            if (isSearchable) {
                AppTopBarWithSearchBar(
                    scrollPosition = scrollPosition,
                    searchBarHint = stringResource(R.string.search_bar_hint, stringResource(id = title).lowercase()),
                    //TODO: implement the search for home once we work on it, for now we do not care
                    searchQuery = "",
                    onSearchQueryChanged = {},
                    onSearchClicked = { },
                    onCloseSearchClicked = { },
                    appTopBar = homeTopBar,
                    content = {
                        homeNavigationGraph()
                    }
                )
            } else {
                Scaffold(topBar = homeTopBar) {
                    homeNavigationGraph()
                }
            }
        }

        if (homeBottomSheetContent != null) {
            WireModalSheetLayout(
                sheetState = homeBottomSheetState,
                sheetContent = homeBottomSheetContent
            ) {
                homeContent()
            }
        } else {
            homeContent()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeNavigationGraph(startScreen: String?, homeState: HomeState) {
    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

    HomeNavigationGraph(
        homeState = homeState,
        navController = homeState.navController,
        startDestination = startDestination
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    currentNavigationItem: HomeNavigationItem,
    onOpenDrawerClicked: () -> Unit,
    onNavigateToUserProfile: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        title = stringResource(id = currentNavigationItem.title),
        onNavigationPressed = onOpenDrawerClicked,
        navigationIconType = NavigationIconType.Menu,
        actions = {
            UserProfileAvatar(avatarUrl = "", status = UserStatus.AVAILABLE) {
                onNavigateToUserProfile()
            }
        },
        elevation = if (currentNavigationItem.isSearchable) 0.dp else dimensions().topBarElevationHeight,
    )
}
