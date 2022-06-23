package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
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
    val homeState = rememberHomeState()

    val snackbarHostState = remember { SnackbarHostState() }
    handleSnackBarMessage(snackbarHostState, viewModel.snackBarMessageState)

    LaunchedEffect(viewModel.savedStateHandle) {
        viewModel.showPendingToast()
    }

    with(homeState) {
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
                scrollPositionProvider = homeState.scrollPositionProvider,
                homeBottomSheetContent = homeState.homeBottomSheetContent,
                snackbarHostState = snackbarHostState,
                homeBottomSheetState = homeState.bottomSheetState,
                homeTopBar = {
                    HomeTopBar(
                        avatarAsset = viewModel.userAvatar.avatarAsset,
                        status = viewModel.userAvatar.status,
                        currentNavigationItem = homeState.currentNavigationItem,
                        syncState = syncViewModel.syncState,
                        onOpenDrawerClicked = ::openDrawer,
                        onNavigateToUserProfile = viewModel::navigateToUserProfile,
                    )
                },
                currentNavigationItem = homeState.currentNavigationItem,
                homeNavigationGraph = {
                    HomeNavigationGraph(
                        homeState = homeState,
                        startScreen = startScreen
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    scrollPositionProvider: (() -> Int)?,
    homeBottomSheetState: ModalBottomSheetState,
    snackbarHostState: SnackbarHostState,
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
                Scaffold(
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                )
                    {
                        Box(modifier = Modifier.padding(it)) {
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
                        }
                }
            } else {
                Scaffold(
                    topBar = homeTopBar,
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                ) {
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
fun HomeNavigationGraph(startScreen: String?, homeState: HomeState) {
    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

    HomeNavigationGraph(
        homeState = homeState,
        navController = homeState.navController,
        startDestination = startDestination
    )
}

@Composable
private fun handleSnackBarMessage(
    snackbarHostState: SnackbarHostState,
    conversationListSnackBarState: HomeSnackBarState?
) {
    conversationListSnackBarState?.let { messageType ->
        val message = when (messageType) {
            is HomeSnackBarState.SuccessConnectionIgnoreRequest ->
                stringResource(id = R.string.connection_request_ignored, messageType.userName)
        }
        LaunchedEffect(messageType) {
            snackbarHostState.showSnackbar(message)
        }
    }
}
