package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel) {
    viewModel.checkRequirements()
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

        ModalDrawer(
            drawerBackgroundColor = MaterialTheme.colorScheme.surface,
            drawerElevation = 0.dp,
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
                        avatarAsset = viewModel.userAvatar,
                        currentNavigationItem = homeState.currentNavigationItem,
                        onOpenDrawerClicked = { openDrawer() },
                        onNavigateToUserProfile = viewModel::navigateToUserProfile,
                    )
                },
                currentNavigationItem = homeState.currentNavigationItem,
                homeNavigationGraph = { HomeNavigationGraph(homeState = homeState, startScreen = startScreen) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
                    // TODO: implement the search for home once we work on it, for now we do not care
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
                    Box(modifier = Modifier.padding(it)) {
                        homeNavigationGraph()
                    }
                }
            }
        }
        WireModalSheetLayout(
            sheetState = homeBottomSheetState,
            coroutineScope = rememberCoroutineScope(),
            // we want to render "nothing" instead of doing a if/else check
            // on homeBottomSheetContent and wrap homeContent() into WireModalSheetLayout
            // or render it without WireModalSheetLayout to avoid
            // recomposing the homeContent() when homeBottomSheetContent
            // changes from null to "something"
            sheetContent = homeBottomSheetContent ?: {}
        ) {
            homeContent()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeNavigationGraph(startScreen: String?, homeState: HomeState) {
    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

    HomeNavigationGraph(
        homeState = homeState,
        navController = homeState.navController,
        startDestination = startDestination
    )
}
