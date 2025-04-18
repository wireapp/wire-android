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
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
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

    val state by viewModel.state.collectAsState()

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        if (searchBarState.searchQueryTextState.text.isNotEmpty()) {
            delay(300)
        }
        viewModel.onSearchQueryUpdated(searchBarState.searchQueryTextState.text.toString())
    }

    val isSearchVisible = when {
        state is CellViewState.Files -> true
        state.isEmptySearch() -> true
        else -> false
    }

    searchBarState.searchVisibleChanged(isSearchVisible)

    CellScreenContent(
        actionsFlow = viewModel.actions,
        viewState = state,
        sendIntent = { viewModel.sendIntent(it) },
        downloadFileState = viewModel.downloadFile,
        fileMenuState = viewModel.menu,
        isAllFiles = true,
        showPublicLinkScreen = { assetId, fileName, linkId ->
            navigator.navigate(
                NavigationCommand(
                    PublicLinkScreenDestination(
                        assetId = assetId,
                        fileName = fileName,
                        publicLinkId = linkId
                    )
                )
            )
        },
    )
}

private fun CellViewState.isEmptySearch() = this is CellViewState.Empty && isSearchResult
