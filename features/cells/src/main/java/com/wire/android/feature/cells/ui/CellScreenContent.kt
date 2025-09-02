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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.common.LoadingScreen
import com.wire.android.feature.cells.ui.dialog.DeleteConfirmationDialog
import com.wire.android.feature.cells.ui.dialog.NodeActionsBottomSheet
import com.wire.android.feature.cells.ui.dialog.RestoreConfirmationDialog
import com.wire.android.feature.cells.ui.download.DownloadFileBottomSheet
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.cells.domain.paging.FileListLoadError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun CellScreenContent(
    actionsFlow: Flow<CellViewAction>,
    pagingListItems: LazyPagingItems<CellNodeUi>,
    sendIntent: (CellViewIntent) -> Unit,
    onFolderClick: (CellNodeUi.Folder) -> Unit,
    downloadFileState: StateFlow<CellNodeUi.File?>,
    menuState: Flow<MenuOptions?>,
    showPublicLinkScreen: (PublicLinkScreenData) -> Unit,
    showRenameScreen: (CellNodeUi) -> Unit,
    showMoveToFolderScreen: (String, String, String) -> Unit,
    showAddRemoveTagsScreen: (CellNodeUi) -> Unit,
    isRestoreInProgress: Boolean,
    isAllFiles: Boolean,
    isRecycleBin: Boolean,
    isSearchResult: Boolean = false,
) {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var deleteConfirmation by remember { mutableStateOf<Pair<CellNodeUi, Boolean>?>((null)) }
    var restoreConfirmation by remember { mutableStateOf<CellNodeUi?>(null) }
    var menu by remember { mutableStateOf<MenuOptions?>(null) }

    val downloadFile by downloadFileState.collectAsState()

    when {
        pagingListItems.isLoading() -> LoadingScreen()
        pagingListItems.isError() -> ErrorScreen((pagingListItems.loadState.refresh as? LoadState.Error)?.error) { pagingListItems.retry() }
        pagingListItems.itemCount == 0 -> EmptyScreen(
            isSearchResult = isSearchResult,
            isAllFiles = isAllFiles,
            isRecycleBin = isRecycleBin,
            onRetry = { pagingListItems.retry() }
        )

        else ->
            CellFilesScreen(
                cellNodes = pagingListItems,
                onItemClick = {
                    when (it) {
                        is CellNodeUi.File -> sendIntent(CellViewIntent.OnFileClick(it))
                        is CellNodeUi.Folder -> onFolderClick(it)
                    }
                },
                onItemMenuClick = { sendIntent(CellViewIntent.OnItemMenuClick(it)) },
//                onRefresh = {
//                    viewModel.loadFiles(pullToRefresh = true)
//                }
            )
    }

    menu?.let { menuOptions ->
        NodeActionsBottomSheet(
            menuOptions = menuOptions,
            onDismiss = { menu = null },
            onAction = { action ->
                menu = null
                sendIntent(CellViewIntent.OnMenuItemActionSelected(menuOptions.node, action))
            }
        )
    }

    downloadFile?.let { file ->
        DownloadFileBottomSheet(
            file = file,
            onDismiss = { sendIntent(CellViewIntent.OnDownloadMenuClosed) },
            onDownload = { sendIntent(CellViewIntent.OnFileDownloadConfirmed(file)) },
        )
    }

    deleteConfirmation?.let { (node, isPermanentDelete) ->
        DeleteConfirmationDialog(
            itemName = node.name ?: "",
            isFolder = node is CellNodeUi.Folder,
            isPermanentDelete = isPermanentDelete,
            onConfirm = {
                sendIntent(CellViewIntent.OnNodeDeleteConfirmed(node))
                deleteConfirmation = null
            },
            onDismiss = {
                deleteConfirmation = null
            }
        )
    }

    restoreConfirmation?.let {
        RestoreConfirmationDialog(
            itemName = it.name ?: "",
            isFolder = it is CellNodeUi.Folder,
            isRestoreInProgress = isRestoreInProgress,
            onConfirm = {
                sendIntent(CellViewIntent.OnNodeRestoreConfirmed(it))
                restoreConfirmation = null
            },
            onDismiss = {
                restoreConfirmation = null
            }
        )
    }

    HandleActions(actionsFlow) { action ->
        when (action) {
            is ShowError -> Toast.makeText(context, action.error.message, Toast.LENGTH_SHORT).show()
            is ShowDeleteConfirmation -> deleteConfirmation = action.node to action.isPermanentDelete
            is ShowRestoreConfirmation -> restoreConfirmation = action.node
            is ShowPublicLinkScreen -> showPublicLinkScreen(
                PublicLinkScreenData(
                    assetId = action.cellNode.uuid,
                    fileName = action.cellNode.name ?: action.cellNode.uuid,
                    linkId = action.cellNode.publicLinkId,
                    isFolder = action.cellNode is CellNodeUi.Folder
                )
            )

            is ShowRenameScreen -> showRenameScreen(action.cellNode)
            is ShowMoveToFolderScreen -> showMoveToFolderScreen(action.currentPath, action.nodeToMovePath, action.uuid)
            is ShowAddRemoveTagsScreen -> showAddRemoveTagsScreen(action.cellNode)
            is RefreshData -> pagingListItems.refresh()
        }
    }

    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            menuState.collect { showMenu ->
                menu = showMenu
            }
        }
    }
}

@Composable
private fun ErrorScreen(error: Throwable?, onRetry: () -> Unit) {

    val isConnectionError = (error as? FileListLoadError)?.isConnectionError ?: false

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
            text = stringResource(
                if (isConnectionError) R.string.file_list_load_network_error_title else R.string.file_list_load_error_title
            ),
            textAlign = TextAlign.Center,
            style = typography().title01,
            color = if (isConnectionError) colorsScheme().onBackground else colorsScheme().error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(
                if (isConnectionError) R.string.file_list_load_network_error else R.string.file_list_load_error
            ),
            textAlign = TextAlign.Center,
            color = if (isConnectionError) colorsScheme().onBackground else colorsScheme().error,
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
    isRecycleBin: Boolean = false,
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
        if (!isSearchResult && !isRecycleBin) {
            Text(
                text = stringResource(R.string.file_list_empty_title),
                style = typography().title01,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(dimensions().spacing16x))
        }
        Text(
            text = when {
                isSearchResult -> stringResource(R.string.file_list_search_empty_message)
                isAllFiles -> stringResource(R.string.file_list_empty_message)
                isRecycleBin -> stringResource(R.string.empty_recycle_bin)
                else -> stringResource(R.string.conversation_file_list_empty_message)
            },
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        )

        if (!isSearchResult && isAllFiles) {
            WirePrimaryButton(
                text = stringResource(R.string.reload),
                onClick = onRetry
            )
        }
    }
}

@MultipleThemePreviews
@Composable
fun PreviewErrorScreen() {
    WireTheme {
        ErrorScreen(
            error = null,
            onRetry = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewNetworkErrorScreen() {
    WireTheme {
        ErrorScreen(
            error = FileListLoadError(true),
            onRetry = {}
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewEmptyScreen() {
    WireTheme {
        EmptyScreen(
            isSearchResult = false,
            isAllFiles = true,
            onRetry = {}
        )
    }
}

internal fun LazyPagingItems<*>.isError(): Boolean =
    loadState.refresh is LoadState.Error && itemSnapshotList.isEmpty()

internal fun LazyPagingItems<*>.isLoading(): Boolean =
    loadState.refresh is LoadState.Loading && itemSnapshotList.isEmpty()
