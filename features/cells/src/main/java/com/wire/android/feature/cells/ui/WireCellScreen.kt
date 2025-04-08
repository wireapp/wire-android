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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.download.DownloadFileBottomSheet
import com.wire.android.feature.cells.ui.model.CellFileUi
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.search.SearchBarState
import kotlinx.coroutines.delay

@Suppress("CyclomaticComplexMethod")
@Composable
fun WireCellScreen(
    searchBarState: SearchBarState,
    showPublicLinkScreen: (String, String, String?) -> Unit,
    viewModel: CellViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    var deleteConfirmation by remember { mutableStateOf<CellFileUi?>(null) }
    var menu by remember { mutableStateOf<MenuOptions?>(null) }
    val downloadFile by viewModel.downloadFile.collectAsState()

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        if (searchBarState.searchQueryTextState.text.isNotEmpty()) {
            delay(300)
        }
        viewModel.onQueryUpdated(searchBarState.searchQueryTextState.text.toString())
    }

    val isSearchVisible = when {
        state is CellViewState.Files -> true
        state.isEmptySearch() -> true
        else -> false
    }

    searchBarState.searchVisibleChanged(isSearchVisible)

    when (val viewState = state) {
        is CellViewState.Loading -> LoadingScreen()
        is CellViewState.Empty -> EmptyScreen(
            isSearchResult = viewState.isSearchResult,
            onRetry = viewModel::loadFiles
        )
        is CellViewState.Error -> ErrorScreen(viewModel::loadFiles)
        is CellViewState.Files ->
            CellScreenContent(
                state = viewState,
                onFileClick = viewModel::onFileClick,
                onFileMenuClick = viewModel::onFileMenuClick,
//                onRefresh = {
//                    viewModel.loadFiles(pullToRefresh = true)
//                }
            )
    }

    menu?.let { menuOptions ->
        FileActionsBottomSheet(
            menuOptions = menuOptions,
            onDismiss = { menu = null },
            onAction = { action ->
                menu = null
                viewModel.onAction(menuOptions.file, action)
            }
        )
    }

    downloadFile?.let { file ->
        DownloadFileBottomSheet(
            file = file,
            onDismiss = {
                viewModel.onDownloadMenuClosed()
            },
            onDownload = {
                viewModel.downloadFile(file)
            }
        )
    }

    deleteConfirmation?.let {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteFile(it)
                deleteConfirmation = null
            },
            onDismiss = {
                deleteConfirmation = null
            }
        )
    }

    LaunchedEffect(Unit) {

        viewModel.loadFiles(clearList = true)

        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.actions.collect { action ->
                when (action) {
                    is ShowError -> Toast.makeText(context, action.error.message, Toast.LENGTH_SHORT).show()
                    is ShowDeleteConfirmation -> deleteConfirmation = action.file
                    is ShowPublicLinkScreen -> showPublicLinkScreen(
                        action.file.uuid,
                        action.file.fileName ?: action.file.uuid,
                        action.file.publicLinkId
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.menu.collect { showMenu ->
                menu = showMenu
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WireCircularProgressIndicator(
            modifier = Modifier.size(dimensions().spacing32x),
            progressColor = colorsScheme().primary
        )
    }
}

@Composable
private fun ErrorScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = dimensions().spacing16x,
            alignment = Alignment.CenterVertically
        )
    ) {
        Text(
            text = stringResource(R.string.file_list_load_error),
            textAlign = TextAlign.Center,
        )

        WireSecondaryButton(
            text = stringResource(R.string.retry),
            onClick = { onRetry() }
        )
    }
}

@Composable
private fun EmptyScreen(
    isSearchResult: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = dimensions().spacing16x,
            alignment = Alignment.CenterVertically
        ),
    ) {
        Text(
            text = if (isSearchResult) {
                stringResource(R.string.file_list_search_empty_message)
            } else {
                stringResource(R.string.file_list_empty_message)
            },
            textAlign = TextAlign.Center,
        )

        if (!isSearchResult) {
            WireSecondaryButton(
                text = stringResource(R.string.reload),
                onClick = onRetry
            )
        }
    }
}

private fun CellViewState.isEmptySearch() = this is CellViewState.Empty && isSearchResult
