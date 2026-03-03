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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.generated.cells.destinations.AddRemoveTagsScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.PublicLinkScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.SearchScreenDestination
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.search.SearchTopBar

/**
 * Show files in all conversations with a search bar.
 */
@Suppress("CyclomaticComplexMethod")
@Composable
fun AllFilesScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    viewModel: CellViewModel = hiltViewModel()
) {
    val pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems()
    val focusRequester = remember { FocusRequester() }

    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                SearchTopBar(
                    modifier = Modifier,
                    isSearchActive = false,
                    searchBarHint = stringResource(R.string.search_label),
                    searchQueryTextState = TextFieldState(),
                    onTap = {
                        navigator.navigate(
                            NavigationCommand(SearchScreenDestination(screenType = DriveSearchScreenType.DRIVE))
                        )
                    },
                    focusRequester = focusRequester,
                )
            }
        },
    ) { innerPadding ->
        CellScreenContent(
            modifier = Modifier.padding(innerPadding),
            actionsFlow = viewModel.actions,
            pagingListItems = pagingListItems,
            sendIntent = { viewModel.sendIntent(it) },
            openFolder = { _, _, _ -> },
            downloadFileState = viewModel.downloadFileSheet,
            menuState = viewModel.menu,
            isAllFiles = true,
            isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
            isDeleteInProgress = viewModel.isDeleteInProgress.collectAsState().value,
            isRecycleBin = viewModel.isRecycleBin(),
            isSearchResult = false,
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
            isRefreshing = viewModel.isPullToRefresh.collectAsState(),
            onRefresh = { viewModel.onPullToRefresh() }
        )
    }
}
