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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.dialog.DeleteConfirmationDialog
import com.wire.android.feature.cells.ui.dialog.FileActionsBottomSheet
import com.wire.android.feature.cells.ui.dialog.FolderActionsBottomSheet
import com.wire.android.feature.cells.ui.download.DownloadFileBottomSheet
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun CellScreenContent(
    actionsFlow: Flow<CellViewAction>,
    pagingListItems: LazyPagingItems<CellNodeUi>,
    sendIntent: (CellViewIntent) -> Unit,
    downloadFileState: StateFlow<CellNodeUi.File?>,
    fileMenuState: Flow<MenuOptions?>,
    showPublicLinkScreen: (String, String, String?) -> Unit,
    isAllFiles: Boolean,
    isSearchResult: Boolean = false,
) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var deleteConfirmation by remember { mutableStateOf<CellNodeUi.File?>(null) }
    var menu by remember { mutableStateOf<MenuOptions?>(null) }

    val downloadFile by downloadFileState.collectAsState()

    when {
        pagingListItems.isLoading() -> LoadingScreen()
        pagingListItems.isError() -> ErrorScreen { pagingListItems.retry() }
        pagingListItems.itemCount == 0 -> EmptyScreen(
            isSearchResult = isSearchResult,
            isAllFiles = isAllFiles,
            onRetry = { pagingListItems.retry() }
        )
        else ->
            CellFilesScreen(
                cellNodes = pagingListItems,
                onItemClick = { sendIntent(CellViewIntent.OnItemClick(it)) },
                onItemMenuClick = { sendIntent(CellViewIntent.OnItemMenuClick(it)) },
//                onRefresh = {
//                    viewModel.loadFiles(pullToRefresh = true)
//                }
            )
    }

    menu?.let { menuOptions ->
        when (menuOptions) {
            is MenuOptions.FileMenuOptions -> {
                FileActionsBottomSheet(
                    menuOptions = menuOptions,
                    onDismiss = { menu = null },
                    onAction = { action ->
                        menu = null
                        sendIntent(CellViewIntent.OnMenuFileActionSelected(menuOptions.cellNodeUi, action))
                    }
                )
            }

            is MenuOptions.FolderMenuOptions -> {
                FolderActionsBottomSheet(
                    menuOptions = menuOptions,
                    onDismiss = { menu = null },
                    onAction = { action ->
                        menu = null
                        sendIntent(CellViewIntent.OnMenuFolderActionSelected(menuOptions.cellNodeUi, action))
                    }
                )
            }
        }
    }

    downloadFile?.let { file ->
        DownloadFileBottomSheet(
            file = file,
            onDismiss = { sendIntent(CellViewIntent.OnDownloadMenuClosed) },
            onDownload = { sendIntent(CellViewIntent.OnFileDownloadConfirmed(file)) },
        )
    }

    deleteConfirmation?.let {
        DeleteConfirmationDialog(
            onConfirm = {
                sendIntent(CellViewIntent.OnFileDeleteConfirmed(it))
                deleteConfirmation = null
            },
            onDismiss = {
                deleteConfirmation = null
            }
        )
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            actionsFlow.collect { action ->
                when (action) {
                    is ShowError -> Toast.makeText(context, action.error.message, Toast.LENGTH_SHORT).show()
                    is ShowDeleteConfirmation -> deleteConfirmation = action.file
                    is ShowPublicLinkScreen -> showPublicLinkScreen(
                        action.file.uuid,
                        action.file.name ?: action.file.uuid,
                        action.file.publicLinkId
                    )
                    is RefreshData -> pagingListItems.refresh()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            fileMenuState.collect { showMenu ->
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
            .padding(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        Text(
            text = stringResource(R.string.file_list_load_error),
            textAlign = TextAlign.Center,
            color = colorsScheme().error,
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        WirePrimaryButton(
            text = stringResource(R.string.retry),
            onClick = { onRetry() }
        )
    }
}

@Composable
private fun EmptyScreen(
    isSearchResult: Boolean = false,
    isAllFiles: Boolean = true,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing16x),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        Text(
            text = if (isSearchResult) {
                stringResource(R.string.file_list_search_empty_message)
            } else {
                if (isAllFiles) {
                    stringResource(R.string.file_list_empty_message)
                } else {
                    stringResource(R.string.conversation_file_list_empty_message)
                }
            },
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        if (!isSearchResult) {
            WirePrimaryButton(
                text = stringResource(R.string.reload),
                onClick = onRetry
            )
        }
    }
}

internal fun LazyPagingItems<*>.isError(): Boolean =
    loadState.refresh is LoadState.Error && itemSnapshotList.isEmpty()

internal fun LazyPagingItems<*>.isLoading(): Boolean =
    loadState.refresh is LoadState.Loading && itemSnapshotList.isEmpty()
