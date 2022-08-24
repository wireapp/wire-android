package com.wire.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalDrawer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wire.android.R
import com.wire.android.navigation.HomeNavigationGraph
import com.wire.android.navigation.HomeNavigationItem
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.WireBottomNavigationBar
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.conversationslist.ConversationListState
import com.wire.android.ui.home.conversationslist.ConversationListViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    syncViewModel: FeatureFlagNotificationViewModel,
    commonTopAppBarViewModel: CommonTopAppBarViewModel,
    conversationListViewModel: ConversationListViewModel, // TODO: move required elements from this one to HomeViewModel?
) {
    homeViewModel.checkRequirements()
    val homeUIState = rememberHomeUIState()
    val coroutineScope = rememberCoroutineScope()
    val homeState = syncViewModel.homeState
    val snackbarHostState = remember { SnackbarHostState() }

    handleSnackBarMessage(snackbarHostState, homeUIState.snackbarState, homeUIState::clearSnackbarMessage)

    LaunchedEffect(homeViewModel.savedStateHandle) {
        homeViewModel.checkPendingSnackbarState()?.let(homeUIState::setSnackBarState)
    }

    val topItems = listOf(HomeNavigationItem.Conversations)
    // TODO: Re-enable once we have Archive & Vault
    // listOf(HomeNavigationItem.Conversations, HomeNavigationItem.Archive, HomeNavigationItem.Vault)

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
                    topItems = topItems,
                    scope = coroutineScope,
                    viewModel = homeViewModel
                )
            },
            gesturesEnabled = drawerState.isOpen
        ) {
            HomeContent(
                homeBottomSheetContent = homeUIState.homeBottomSheetContent,
                homeBottomSheetState = homeUIState.bottomSheetState,
                snackbarHostState = snackbarHostState,
                homeTopBar = { elevation ->
                    Column {
                        CommonTopAppBar(commonTopAppBarViewModel = commonTopAppBarViewModel) // as CommonTopAppBarViewModel)
                        HomeTopBar(
                            avatarAsset = homeViewModel.userAvatar.avatarAsset,
                            status = homeViewModel.userAvatar.status,
                            title = stringResource(id = homeUIState.currentNavigationItem.title),
                            elevation = elevation,
                            onOpenDrawerClicked = ::openDrawer,
                            onNavigateToUserProfile = homeViewModel::navigateToUserProfile,
                        )
                    }
                },
                currentNavigationItem = homeUIState.currentNavigationItem,
                homeNavigationGraph = {
                    HomeNavigationGraph(
                        homeUIState = homeUIState,
                        navController = homeUIState.navController,
                        startDestination = topItems[0]
                    )
                },
                openNewConversation = conversationListViewModel::openNewConversation,
                conversationListState = conversationListViewModel.state,
                navController = homeUIState.navController,
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
    homeBottomSheetState: ModalBottomSheetState,
    snackbarHostState: SnackbarHostState,
    homeBottomSheetContent: @Composable (ColumnScope.() -> Unit)?,
    currentNavigationItem: HomeNavigationItem,
    homeNavigationGraph: @Composable () -> Unit,
    homeTopBar: @Composable (elevation: Dp) -> Unit,
    openNewConversation: () -> Unit,
    conversationListState: ConversationListState,
    navController: NavHostController,
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
            CollapsingTopBarScaffold(
                snapOnFling = false,
                keepElevationWhenCollapsed = true,
                topBarHeader = homeTopBar,
                snackbarHost = {
                    SwipeDismissSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                topBarCollapsing = {
                    if (currentNavigationItem.isSearchable)
                        SearchTopBar(
                            isSearchActive = false, // TODO
                            isSearchBarCollapsed = false, // TODO
                            searchBarHint = stringResource(R.string.search_bar_hint, stringResource(id = title).lowercase()),
                            searchQuery = TextFieldValue(""), // TODO
                            onSearchQueryChanged = { /* TODO */ },
                            onInputClicked = { /* TODO */ },
                            onCloseSearchClicked = { /* TODO */ },
                        )
                },
                content = { homeNavigationGraph() },
                floatingActionButton = {
                    if (currentNavigationItem.withNewConversationFab)
                        FloatingActionButton(
                            text = stringResource(R.string.label_new),
                            icon = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_conversation),
                                    contentDescription = stringResource(R.string.content_description_new_conversation),
                                    contentScale = ContentScale.FillBounds,
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier
                                        .padding(start = dimensions().spacing4x, top = dimensions().spacing2x)
                                        .size(dimensions().fabIconSize)
                                )
                            },
                            onClick = openNewConversation
                        )
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = currentNavigationItem.withBottomTabs,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                    ) {
                        WireBottomNavigationBar(
                            HomeNavigationItem.bottomTabItems.toBottomNavigationItems(conversationListState),
                            navController
                        )
                    }
                }
            )
        }
    }
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
            is HomeSnackbarState.BlockingUserOperationSuccess ->
                stringResource(id = R.string.blocking_user_success, messageType.userName)
            HomeSnackbarState.MutingOperationError -> stringResource(id = R.string.error_updating_muting_setting)
            HomeSnackbarState.BlockingUserOperationError -> stringResource(id = R.string.error_blocking_user)
            HomeSnackbarState.None -> ""
            is HomeSnackbarState.DeletedConversationGroupSuccess ->
                stringResource(id = R.string.conversation_group_removed_success, messageType.groupName)
            HomeSnackbarState.LeftConversationSuccess -> stringResource(id = R.string.left_conversation_group_success)
            HomeSnackbarState.LeaveConversationError -> stringResource(id = R.string.leave_group_conversation_error)
            HomeSnackbarState.DeleteConversationGroupError -> stringResource(id = R.string.delete_group_conversation_error)
            HomeSnackbarState.ClearGroupConversationContentSuccess -> stringResource(R.string.group_content_deleted)
            HomeSnackbarState.ClearPrivateConverstaionContentSuccess -> stringResource(R.string.conversation_content_deleted)
        }
        LaunchedEffect(messageType) {
            if (messageType != HomeSnackbarState.None) {
                snackbarHostState.showSnackbar(message)
                onMessageShown()
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
private fun List<HomeNavigationItem>.toBottomNavigationItems(
    uiListState: ConversationListState
): List<WireBottomNavigationItemData> = map { homeNavigationItem ->
    when (homeNavigationItem) {
        HomeNavigationItem.Conversations -> homeNavigationItem.toBottomNavigationItemData(uiListState.newActivityCount)
        HomeNavigationItem.Calls -> homeNavigationItem.toBottomNavigationItemData(uiListState.missedCallsCount)
        HomeNavigationItem.Mentions -> homeNavigationItem.toBottomNavigationItemData(uiListState.unreadMentionsCount)
        else -> homeNavigationItem.toBottomNavigationItemData(0L)
    }
}
