/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.app.destinations.GlobalCellsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.app.navgraphs.HomeGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.destination
import com.wire.android.feature.cells.ui.cellViewModel
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeDestination.FabOptions
import com.wire.android.navigation.rememberWireNavHostEngine
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.button.FloatingActionButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.onEscapeOrBackKey
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.home.drawer.HomeDrawer
import com.wire.android.ui.home.drawer.HomeDrawerState

@Composable
internal fun HomeDrawerSheet(
    currentRoute: String,
    homeDrawerState: HomeDrawerState,
    focusTrapState: HomeDrawerSheetFocusTrapState,
    focusTrapActions: HomeDrawerSheetFocusTrapActions,
    firstItemFocusRequester: FocusRequester,
    lastItemFocusRequester: FocusRequester,
    onNavigateToHomeItem: (HomeDestination) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    BoxWithConstraints {
        val width = min(maxWidth - dimensions().homeDrawerSheetEndPadding, DrawerDefaults.MaximumDrawerWidth)
        ModalDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            drawerTonalElevation = 0.dp,
            drawerShape = RectangleShape,
            modifier = Modifier
                .width(width)
                .homeDrawerSheetFocusTrap(
                    state = focusTrapState,
                    actions = focusTrapActions
                )
        ) {
            HomeDrawer(
                currentRoute = currentRoute,
                homeDrawerState = homeDrawerState,
                navigateToHomeItem = onNavigateToHomeItem,
                onCloseDrawer = onCloseDrawer,
                isFocusTrapEnabled = focusTrapState.enabled,
                firstItemFocusRequester = firstItemFocusRequester,
                lastItemFocusRequester = lastItemFocusRequester
            )
        }
    }
}

@Stable
internal class HomeScaffoldState(val holder: HomeStateHolder)

internal data class HomeScaffoldFocusRequesters(
    val search: FocusRequester,
    val fab: FocusRequester,
)

internal data class HomeScaffoldActions(
    val onDrawerItemFocusRequested: (isShiftPressed: Boolean) -> Unit,
    val onNewConversationClick: () -> Unit,
    val onSelfUserClick: () -> Unit,
    val onHamburgerMenuClick: () -> Unit,
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun HomeScaffold(
    homeState: HomeState,
    state: HomeScaffoldState,
    drawerState: DrawerState,
    focusRequesters: HomeScaffoldFocusRequesters,
    actions: HomeScaffoldActions,
) {
    with(state.holder) {
        CollapsingTopBarScaffold(
            modifier = Modifier.homeScaffoldFocusTrap(
                isDrawerOpen = drawerState.isOpen,
                onDrawerItemFocusRequested = actions.onDrawerItemFocusRequested
            ),
            snapOnFling = false,
            topBarHeader = {
                HomeTopBarHeader(
                    homeState = homeState,
                    state = state,
                    searchFocusRequester = focusRequesters.search,
                    onSelfUserClick = actions.onSelfUserClick,
                    onHamburgerMenuClick = actions.onHamburgerMenuClick
                )
            },
            topBarCollapsing = {
                HomeSearchTopBar(
                    state = state,
                    searchFocusRequester = focusRequesters.search,
                    fabFocusRequester = focusRequesters.fab
                )
            },
            collapsingEnabled = !searchBarState.isSearchActive,
            contentLazyListState = lazyListStateFor(currentNavigationItem, currentConversationFilter),
            content = {
                HomeNavHost(state = state)
            },
            floatingActionButton = {
                HomeScaffoldFloatingActionButton(
                    state = state,
                    fabFocusRequester = focusRequesters.fab,
                    onNewConversationClick = actions.onNewConversationClick
                )
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun HomeTopBarHeader(
    homeState: HomeState,
    state: HomeScaffoldState,
    searchFocusRequester: FocusRequester,
    onSelfUserClick: () -> Unit,
    onHamburgerMenuClick: () -> Unit,
) {
    with(state.holder) {
        AnimatedVisibility(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            visible = !searchBarState.isSearchActive,
            enter = fadeIn() + expandVertically(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            HomeTopBar(
                title = currentTitle.asString(),
                currentConversationFilter = currentConversationFilter,
                navigationItem = currentNavigationItem,
                userAvatarData = homeState.userAvatarData,
                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold manages applied elevation
                withLegalHoldIndicator = homeState.shouldDisplayLegalHoldIndicator,
                shouldShowCreateTeamUnreadIndicator = homeState.shouldShowCreateTeamUnreadIndicator,
                onHamburgerMenuClick = onHamburgerMenuClick,
                onNavigateToSelfUserProfile = onSelfUserClick,
                onOpenConversationFilter = {
                    conversationsFilterBottomSheetState.show(Unit)
                },
                nextFocusRequester = searchFocusRequester,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun HomeSearchTopBar(
    state: HomeScaffoldState,
    searchFocusRequester: FocusRequester,
    fabFocusRequester: FocusRequester,
) {
    with(state.holder) {
        currentNavigationItem.searchBar?.let { searchBar ->
            AnimatedVisibility(
                visible = searchBarState.isSearchVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    searchBarHint = stringResource(searchBar.hint),
                    searchQueryTextState = searchBarState.searchQueryTextState,
                    onCloseSearchClicked = searchBarState::closeSearch,
                    onActiveChanged = { isFocused ->
                        if (isFocused) {
                            searchBarState.openSearch()
                        }
                    },
                    externalFocusRequester = searchFocusRequester,
                    nextFocusRequester = when {
                        searchBarState.isSearchActive -> emptySearchResultFocusRequester
                        currentNavigationItem.fab != null -> fabFocusRequester
                        else -> firstConversationFocusRequester
                    },
                    activateSearchOnFocus = false,
                )
            }
        }
    }
}

@Composable
private fun HomeNavHost(state: HomeScaffoldState) {
    /**
     * This "if" is a workaround, otherwise it can crash because of the SubcomposeLayout's nature.
     * We need to communicate to the sub-compositions when they are to be disposed by the parent and ignore
     * compositions in the round they are to be disposed. More here:
     * https://github.com/google/accompanist/issues/1487
     * https://issuetracker.google.com/issues/268422136
     * https://issuetracker.google.com/issues/254645321
     */
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (lifecycleState != Lifecycle.State.DESTROYED) {
        val navHostEngine = rememberWireNavHostEngine()
        DestinationsNavHost(
            navGraph = HomeGraph,
            start = HomeGraph.defaultStartDirection,
            engine = navHostEngine,
            navController = state.holder.navController,
            dependenciesContainerBuilder = {
                dependency(state.holder)

                // Scope CellViewModel to HomeScreen so SearchScreen can reuse it via previousBackStackEntry.
                destination(GlobalCellsScreenDestination) {
                    val parentEntry = remember(navBackStackEntry) {
                        state.holder.navigator.navController.getBackStackEntry(HomeScreenDestination.route)
                    }
                    dependency(cellViewModel(parentEntry))
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun HomeScaffoldFloatingActionButton(
    state: HomeScaffoldState,
    fabFocusRequester: FocusRequester,
    onNewConversationClick: () -> Unit,
) {
    with(state.holder) {
        AnimatedVisibility(
            visible = currentNavigationItem.fab != null && !searchBarState.isSearchActive,
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            HomeFloatingActionButton(
                currentNavigationItem = currentNavigationItem,
                fabFocusRequester = fabFocusRequester,
                nextFocusRequester = firstConversationFocusRequester,
                onNewConversationClick = onNewConversationClick,
                onNewMeetingClick = { newMeetingBottomSheetState.show(Unit) }
            )
        }
    }
}

internal data class HomeDrawerSheetFocusTrapState(
    val enabled: Boolean,
    val focusRequester: FocusRequester,
    val isSheetFocused: Boolean,
)

internal data class HomeDrawerSheetFocusTrapActions(
    val onSheetFocusChanged: (Boolean) -> Unit,
    val onItemFocusRequested: (isShiftPressed: Boolean) -> Unit,
    val onClose: () -> Unit,
)

internal fun Modifier.homeDrawerKeyboardNavigation(
    isDrawerOpen: Boolean,
    isDrawerSheetFocused: Boolean,
    onDrawerItemFocusRequested: (isShiftPressed: Boolean) -> Unit,
    onCloseDrawer: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    val isKeyDown = event.type == KeyEventType.KeyDown
    val isTabKeyDown = isKeyDown && event.key == Key.Tab
    val isCloseKeyDown = isKeyDown && (event.key == Key.Escape || event.key == Key.Back)

    when {
        isDrawerOpen && isCloseKeyDown -> {
            onCloseDrawer()
            true
        }

        isDrawerOpen && isDrawerSheetFocused && isTabKeyDown -> {
            onDrawerItemFocusRequested(event.isShiftPressed)
            true
        }

        else -> false
    }
}

private fun Modifier.homeScaffoldFocusTrap(
    isDrawerOpen: Boolean,
    onDrawerItemFocusRequested: (isShiftPressed: Boolean) -> Unit,
): Modifier = focusProperties {
    canFocus = !isDrawerOpen
}
    .onPreviewKeyEvent { event ->
        val isTabKeyDown = event.type == KeyEventType.KeyDown && event.key == Key.Tab
        if (isDrawerOpen && isTabKeyDown) {
            onDrawerItemFocusRequested(event.isShiftPressed)
            true
        } else {
            false
        }
    }
    .then(
        if (isDrawerOpen) {
            Modifier.clearAndSetSemantics { }
        } else {
            Modifier
        }
    )

private fun Modifier.homeDrawerSheetFocusTrap(
    state: HomeDrawerSheetFocusTrapState,
    actions: HomeDrawerSheetFocusTrapActions,
): Modifier = this
    .semantics {
        isTraversalGroup = true
        traversalIndex = -1f
    }
    .focusRequester(state.focusRequester)
    .focusProperties {
        onExit = {
            if (state.enabled) {
                cancelFocusChange()
            }
        }
    }
    .onFocusChanged {
        actions.onSheetFocusChanged(it.isFocused)
    }
    .onPreviewKeyEvent { event ->
        val isTabKeyDown = event.type == KeyEventType.KeyDown && event.key == Key.Tab
        if (state.enabled && state.isSheetFocused && isTabKeyDown) {
            actions.onItemFocusRequested(event.isShiftPressed)
            true
        } else {
            false
        }
    }
    .onEscapeOrBackKey(
        enabled = state.enabled,
        onKeyPressed = actions.onClose
    )
    .focusGroup()
    .focusable()

@Composable
private fun HomeFloatingActionButton(
    currentNavigationItem: HomeDestination,
    fabFocusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onNewConversationClick: () -> Unit,
    onNewMeetingClick: () -> Unit,
) {
    var currentFab by remember { mutableStateOf(currentNavigationItem.fab ?: FabOptions.NewConversation) }
    // to keep the fab during the exit animation, we need to keep last known (non-null) fab data
    currentNavigationItem.fab?.let { currentFab = it }

    FloatingActionButton(
        text = stringResource(currentFab.text),
        modifier = Modifier
            .focusRequester(fabFocusRequester)
            .focusProperties {
                next = nextFocusRequester
            },
        icon = {
            Image(
                painter = painterResource(currentFab.icon),
                contentDescription = stringResource(currentFab.contentDescription),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier
                    .padding(start = dimensions().spacing4x, top = dimensions().spacing2x)
                    .size(dimensions().fabIconSize)
            )
        },
        onClick = {
            when (currentNavigationItem.fab) {
                FabOptions.NewConversation -> onNewConversationClick()
                FabOptions.NewMeeting -> onNewMeetingClick()
                else -> { /* no-op */ }
            }
        }
    )
}
