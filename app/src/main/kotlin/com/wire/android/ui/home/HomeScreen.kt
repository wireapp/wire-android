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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.home.sync.SyncStateViewModel

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(startScreen: String?, viewModel: HomeViewModel, syncViewModel: SyncStateViewModel) {
    viewModel.checkRequirements()
    val homeUIState = rememberHomeUIState()
    val coroutineScope = rememberCoroutineScope()
    val homeState = viewModel.homeState

    with(homeUIState) {
        ModalDrawer(
            drawerBackgroundColor = MaterialTheme.colorScheme.surface,
            drawerElevation = 0.dp,
            drawerShape = RectangleShape,
            drawerState = drawerState,
            drawerContent = {
                HomeDrawer(
                    drawerState = drawerState,
                    currentRoute = currentNavigationItem.route,
                    homeNavController = navController,
                    topItems = HomeNavigationItem.all,
                    scope = coroutineScope,
                    viewModel = viewModel
                )
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            HomeContent(
                scrollPositionProvider = homeUIState.scrollPositionProvider,
                homeBottomSheetContent = homeUIState.homeBottomSheetContent,
                homeBottomSheetState = homeUIState.bottomSheetState,
                homeTopBar = {
                    HomeTopBar(
                        avatarAsset = viewModel.userAvatar.avatarAsset,
                        status = viewModel.userAvatar.status,
                        currentNavigationItem = homeUIState.currentNavigationItem,
                        syncState = syncViewModel.syncState,
                        onOpenDrawerClicked = ::openDrawer,
                        onNavigateToUserProfile = viewModel::navigateToUserProfile,
                    )
                },
                currentNavigationItem = homeUIState.currentNavigationItem,
                homeNavigationGraph = {
                    HomeNavigationGraph(
                        homeState = homeUIState,
                        startScreen = startScreen
                    )
                }
            )
        }
        if (homeState.showFileSharingDialog) {
            val text: String = if (homeState.isFileSharingEnabledState) {
                stringResource(id = R.string.sharing_files_enabled)
            } else {
                stringResource(id = R.string.sharing_files_disabled)
            }

            WireDialog(
                title = stringResource(id = R.string.there_has_been_a_change),
                text = text,
                onDismiss = { viewModel.hideDialogStatus() },
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = { viewModel.hideDialogStatus() },
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    scrollPositionProvider: (() -> Int)?,
    homeBottomSheetState: ModalBottomSheetState,
    homeBottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    currentNavigationItem: HomeNavigationItem,
    homeNavigationGraph: @Composable () -> Unit,
    homeTopBar: @Composable () -> Unit
) {
    with(currentNavigationItem) {
        WireModalSheetLayout(
            sheetState = homeBottomSheetState,
            coroutineScope = rememberCoroutineScope(),
            // we want to render "nothing" instead of doing a if/else check
            // on homeBottomSheetContent and wrap homeContent() into WireModalSheetLayout
            // or render it without WireModalSheetLayout to avoid
            // recomposing the homeContent() when homeBottomSheetContent
            // changes from null to "something"
            sheetContent = homeBottomSheetContent ?: { }
        ) {
            // TODO(): Enable top search bar
            if (false) {
                AppTopBarWithSearchBar(
                    scrollPositionProvider = scrollPositionProvider,
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
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeNavigationGraph(startScreen: String?, homeState: HomeUIState) {
    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

    HomeNavigationGraph(
        homeUIState = homeState,
        navController = homeState.navController,
        startDestination = startDestination
    )
}
