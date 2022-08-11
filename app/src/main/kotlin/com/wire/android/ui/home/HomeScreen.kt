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
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
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
    val homeUIState = rememberHomeUIState()
    val coroutineScope = rememberCoroutineScope()
    val homeState = syncViewModel.homeState
    val snackbarHostState = remember { SnackbarHostState() }

    handleSnackBarMessage(snackbarHostState, homeUIState.snackbarState, homeUIState::clearSnackbarMessage)

    LaunchedEffect(viewModel.savedStateHandle) {
        viewModel.checkPendingSnackbarState()?.let(homeUIState::setSnackBarState)
    }

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
                snackbarHostState = snackbarHostState,
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
                title = stringResource(id = R.string.team_settings_changed),
                text = text,
                onDismiss = { syncViewModel.hideDialogStatus() },
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = { syncViewModel.hideDialogStatus() },
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
                            // Disable as for now
//                            scrollPositionProvider = scrollPositionProvider,
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
fun HomeNavigationGraph(startScreen: String?, homeState: HomeUIState) {
    val startDestination = HomeNavigationItem.all.firstOrNull { startScreen == it.route }?.route

    HomeNavigationGraph(
        homeUIState = homeState,
        navController = homeState.navController,
        startDestination = startDestination
    )
}

@Composable
private fun handleSnackBarMessage(
    snackbarHostState: SnackbarHostState,
    conversationListSnackBarState: HomeSnackbarState,
    onMessageShown: () -> Unit
) {
    conversationListSnackBarState.let { messageType ->
        val message = when (messageType) {
            is HomeSnackbarState.SuccessConnectionIgnoreRequest ->
                stringResource(id = R.string.connection_request_ignored, messageType.userName)
            is HomeSnackbarState.BlockingUserOperationSuccess -> stringResource(id = R.string.blocking_user_success, messageType.userName)
            HomeSnackbarState.MutingOperationError -> stringResource(id = R.string.error_updating_muting_setting)
            HomeSnackbarState.BlockingUserOperationError -> stringResource(id = R.string.error_blocking_user)
            HomeSnackbarState.None -> ""
        }
        LaunchedEffect(messageType) {
            if (messageType != HomeSnackbarState.None) {
                snackbarHostState.showSnackbar(message)
                onMessageShown()
            }
        }
    }
}
