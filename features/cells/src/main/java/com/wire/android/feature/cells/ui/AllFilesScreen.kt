/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.BulkSelectionHint
import com.wire.android.feature.cells.ui.common.OfflineAwareSortRow
import com.wire.android.feature.cells.ui.common.OfflineBanner
import com.wire.android.feature.cells.ui.dialog.BulkOfflineDialogs
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.toNavArg
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun AllFilesScreen(
    navigationActions: AllFilesNavigationActions,
    modifier: Modifier = Modifier,
    viewModel: CellViewModel = cellViewModel(CellFilesNavArgs()),
) {
    val pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems()
    val isOnlineState by viewModel.isOnline.collectAsState()
    // When offline files are disabled, never enter offline mode so all offline UI stays hidden.
    val isOnline = isOnlineState || !viewModel.offlineFilesEnabled

    val sortingCriteria by viewModel.sortingCriteria.collectAsState()

    val selectedNodes by viewModel.selectedNodes.collectAsState()
    val isSelectionMode = selectedNodes.isNotEmpty()
    val bulkOfflineDialog by viewModel.bulkOfflineDialog.collectAsState()
    val canMakeSelectionAvailableOffline by viewModel.canMakeSelectionAvailableOffline.collectAsState()
    val canRemoveSelectionOfflineAccess by viewModel.canRemoveSelectionOfflineAccess.collectAsState()
    val showBulkSelectionHint by viewModel.showBulkSelectionHint.collectAsState()
    val isOfflineOnly by viewModel.isOfflineOnly.collectAsState()
    val showOfflineOnlyToggle by viewModel.showOfflineOnlyToggle.collectAsState()
    val showOfflineToggleHint by viewModel.showOfflineToggleHint.collectAsState()
    val bulkActions = remember(canMakeSelectionAvailableOffline, canRemoveSelectionOfflineAccess) {
        allFilesBulkActions(
            canMakeAvailableOffline = canMakeSelectionAvailableOffline,
            canRemoveOfflineAccess = canRemoveSelectionOfflineAccess,
        )
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.sendIntent(CellViewIntent.OnClearSelection)
    }

    bulkOfflineDialog?.let { dialog ->
        BulkOfflineDialogs(
            dialog = dialog,
            onConfirm = { viewModel.sendIntent(CellViewIntent.OnBulkOfflineConfirmed) },
            onDismiss = { viewModel.sendIntent(CellViewIntent.OnBulkOfflineDialogDismissed) },
        )
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(sortingCriteria) {
        lazyListState.animateScrollToItem(0)
    }

    val topBarState = when {
        !isOnline -> AllFilesTopBarState.Offline
        isSelectionMode -> AllFilesTopBarState.Selection
        else -> AllFilesTopBarState.Browsing
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                AnimatedContent(
                    targetState = topBarState,
                    // The default transition, without clipping: the offline toggle pulses beyond the header's bounds.
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith fadeOut(
                                    animationSpec = tween(durationMillis = 90)
                                )
                                ).using(SizeTransform(clip = false))
                    },
                    label = "all_files_top_bar",
                ) { state ->
                    when (state) {
                        AllFilesTopBarState.Browsing -> Column {
                            SearchTopBar(
                                modifier = Modifier,
                                isSearchActive = false,
                                searchBarHint = stringResource(R.string.search_label),
                                searchQueryTextState = rememberTextFieldState(),
                                onTap = { navigationActions.openSearch(sortingCriteria.toNavArg()) },
                            )
                            // Only shown while browsing; with fewer than two items there is nothing to select several of.
                            AnimatedVisibility(
                                visible = showBulkSelectionHint && pagingListItems.itemCount >= 2,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                            ) {
                                BulkSelectionHint(onDismiss = viewModel::onBulkSelectionHintDismissed)
                            }
                            OfflineAwareSortRow(
                                sortingCriteria = sortingCriteria,
                                screenType = DriveSearchScreenType.DRIVE,
                                isOfflineOnly = isOfflineOnly,
                                showOfflineToggle = showOfflineOnlyToggle,
                                onSortByClicked = { viewModel.setSortBy(it) },
                                onOrderClicked = { viewModel.setSorting(it) },
                                onOfflineOnlyChange = viewModel::setOfflineOnly,
                                showOfflineToggleHint = showOfflineToggleHint,
                                onOfflineToggleHintDismiss = viewModel::onOfflineToggleHintDismissed,
                            )
                        }

                        // The home top bar above stays as is: the selection gets its own bar, which also clears it.
                        AllFilesTopBarState.Selection -> WireCenterAlignedTopAppBar(
                            onNavigationPressed = { viewModel.sendIntent(CellViewIntent.OnClearSelection) },
                            titleContent = {
                                // At least one: while this bar animates out the selection is already empty, and the
                                // count must not roll back to a title of its own.
                                SelectionAwareTitle(
                                    title = "",
                                    selectedCount = selectedNodes.size.coerceAtLeast(1),
                                )
                            },
                            navigationIconType = NavigationIconType.Close(R.string.cells_bulk_selection_cancel_content_description),
                            elevation = dimensions().spacing0x,
                        )

                        AllFilesTopBarState.Offline -> OfflineBanner()
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            CellScreenContent(
                modifier = Modifier.fillMaxSize(),
                actionsFlow = viewModel.actions,
                pagingListItems = pagingListItems,
                sendIntent = { viewModel.sendIntent(it) },
                openFolder = { _, _, _ -> },
                menuState = viewModel.menu,
                isAllFiles = true,
                isOffline = !isOnline,
                isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
                isDeleteInProgress = viewModel.isDeleteInProgress.collectAsState().value,
                isRecycleBin = viewModel.isRecycleBin(),
                isSearchResult = false,
                showPublicLinkScreen = navigationActions.showPublicLink,
                showMoveToFolderScreen = { _, _, _ -> },
                showBulkMoveToFolderScreen = { _, _ -> },
                showRenameScreen = {},
                showAddRemoveTagsScreen = navigationActions.showAddRemoveTags,
                isRefreshing = viewModel.isPullToRefresh.collectAsState(),
                onRefresh = { viewModel.onPullToRefresh() },
                showImageViewer = navigationActions.showImageViewer,
                showVideoViewer = navigationActions.showVideoPlayer,
                showAudioPlayer = navigationActions.showAudioPlayer,
                showPdfViewer = navigationActions.showPdfViewer,
                fileReadyFlow = viewModel.fileReadyFlow,
                showViewerOnlyIcon = viewModel.drivePermissionsEnabled,
                sortBy = sortingCriteria.by,
                selectedNodeIds = selectedNodes.keys,
                isSelectionEnabled = viewModel.isBulkSelectionEnabled.collectAsState().value,
                isOfflineOnly = isOfflineOnly && isOnline,
                isPullToRefreshEnabled = !isOfflineOnly,
            )
            // Drawn over the list rather than as the scaffold's bottom bar, so showing or hiding it doesn't resize the list.
            // Hidden while the selection holds nothing the offline actions apply to.
            AnimatedVisibility(
                visible = isSelectionMode && bulkActions.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensions().spacing16x),
                enter = slideInVertically { height -> height } + fadeIn(),
                exit = slideOutVertically { height -> height } + fadeOut(),
            ) {
                CellBulkActionsBar(
                    actions = bulkActions,
                    onActionClick = { action -> viewModel.sendIntent(action.toIntent()) },
                )
            }
        }
    }
}

private enum class AllFilesTopBarState { Browsing, Selection, Offline }

// All Files spans conversations and has no folder of its own, so only the offline actions apply here.
private fun allFilesBulkActions(
    canMakeAvailableOffline: Boolean,
    canRemoveOfflineAccess: Boolean,
): ImmutableList<CellBulkAction> =
    listOfNotNull(
        CellBulkAction.MakeAvailableOffline.takeIf { canMakeAvailableOffline },
        CellBulkAction.RemoveOfflineAccess.takeIf { canRemoveOfflineAccess },
    ).toPersistentList()
