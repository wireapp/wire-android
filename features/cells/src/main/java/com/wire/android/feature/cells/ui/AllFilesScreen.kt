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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.ui.destinations.AddRemoveTagsScreenDestination
import com.wire.android.feature.cells.ui.destinations.ConversationFilesScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.ui.filter.FilterBottomSheet
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.ui.common.search.SearchBarState
import kotlinx.coroutines.delay

/**
 * Show files in all conversations with a search bar.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
fun AllFilesScreen(
    navigator: WireNavigator,
    searchBarState: SearchBarState,
    viewModel: CellViewModel = hiltViewModel(),
) {
    val pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems()

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        if (searchBarState.searchQueryTextState.text.isNotEmpty()) {
            delay(300)
        }
        viewModel.onSearchQueryUpdated(searchBarState.searchQueryTextState.text.toString())
    }

    val isSearchVisible = when {
        pagingListItems.isLoading() -> false
        pagingListItems.isError() -> false
        pagingListItems.itemCount == 0 && !viewModel.hasSearchQuery() -> false
        else -> true
    }

    searchBarState.searchVisibleChanged(isSearchVisible)
    CellScreenContent(
        actionsFlow = viewModel.actions,
        pagingListItems = pagingListItems,
        sendIntent = { viewModel.sendIntent(it) },
        onFolderClick = {
            navigator.navigate(NavigationCommand(ConversationFilesScreenDestination(it.remotePath)))
        },
        downloadFileState = viewModel.downloadFileSheet,
        menuState = viewModel.menu,
        isAllFiles = true,
        isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
        isRecycleBin = viewModel.isRecycleBin(),
        isSearchResult = viewModel.hasSearchQuery(),
        showPublicLinkScreen = { publicLinkScreenData ->
            navigator.navigate(
                NavigationCommand(
                    PublicLinkScreenDestination(
                        assetId = publicLinkScreenData.assetId,
                        fileName = publicLinkScreenData.fileName,
                        publicLinkId = publicLinkScreenData.linkId,
                        isFolder = publicLinkScreenData.isFolder
                    )
                )
            )
        },
        showMoveToFolderScreen = { _, _, _ -> },
        showRenameScreen = {},
        showAddRemoveTagsScreen = { node ->
            navigator.navigate(
                NavigationCommand(
                    AddRemoveTagsScreenDestination(node.uuid, node.tags.toCollection(ArrayList()))
                )
            )
        },
    )

    if (searchBarState.isFilterActive) {

        viewModel.loadTags()

        FilterBottomSheet(
            selectableTags = viewModel.tags.collectAsState().value,
            selectedTags = viewModel.selectedTags.collectAsState().value,
            onApply = {
                searchBarState.onFilterActiveChanged(false)
                viewModel.updateSelectedTags(it)
            },
            onClearAll = {
                viewModel.updateSelectedTags(emptySet())
            },
            onDismiss = { searchBarState.onFilterActiveChanged(false) },
        )
    }
}
