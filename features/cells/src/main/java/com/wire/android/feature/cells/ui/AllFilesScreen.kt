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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.OfflineBanner
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.feature.cells.ui.search.sort.toNavArg
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.search.SearchTopBar

@Suppress("CyclomaticComplexMethod")
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

    val lazyListState = rememberLazyListState()
    LaunchedEffect(sortingCriteria) {
        lazyListState.animateScrollToItem(0)
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                AnimatedContent(isOnline) {
                    if (it) {
                        Column {
                            SearchTopBar(
                                modifier = Modifier,
                                isSearchActive = false,
                                searchBarHint = stringResource(R.string.search_label),
                                searchQueryTextState = rememberTextFieldState(),
                                onTap = { navigationActions.openSearch(sortingCriteria.toNavArg()) },
                                )
                            SortRowWithMenu(
                                sortingCriteria = sortingCriteria,
                                screenType = DriveSearchScreenType.DRIVE,
                                onSortByClicked = { viewModel.setSortBy(it) },
                                onOrderClicked = { viewModel.setSorting(it) },
                            )
                        }
                    } else {
                        OfflineBanner()
                    }
                }
            }
        },
    ) { innerPadding ->
        CellScreenContent(
            modifier = Modifier.padding(innerPadding),
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
            showRenameScreen = {},
            showAddRemoveTagsScreen = navigationActions.showAddRemoveTags,
            isRefreshing = viewModel.isPullToRefresh.collectAsState(),
            onRefresh = { viewModel.onPullToRefresh() },
            showImageViewer = navigationActions.showImageViewer,
            showVideoViewer = navigationActions.showVideoPlayer,
            showAudioPlayer = navigationActions.showAudioPlayer,
            fileReadyFlow = viewModel.fileReadyFlow,
        )
    }
}
