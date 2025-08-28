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

import android.content.Context
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.common.functional.fold
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class CellViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val download: DownloadCellFileUseCase,
    private val isCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val fileHelper: FileHelper,
    private val kaliumFileSystem: KaliumFileSystem,
    @ApplicationContext val context: Context
) : ActionsViewModel<CellViewAction>() {

    private val navArgs: CellFilesNavArgs = savedStateHandle.navArgs()

    // Show menu with actions for the selected file.
    private val _menu: MutableSharedFlow<MenuOptions> = MutableSharedFlow()
    internal val menu = _menu.asSharedFlow()

    // Show bottom sheet with download progress.
    private val _downloadFileSheet: MutableStateFlow<CellNodeUi.File?> = MutableStateFlow(null)
    internal val downloadFileSheet = _downloadFileSheet.asStateFlow()
    val isLoading = MutableStateFlow(false)

    // Download progress value for each file being downloaded.
    private val downloadDataFlow = MutableStateFlow<Map<String, DownloadData>>(emptyMap())

    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")

    private val removedItemsFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()

    private val _tags = MutableStateFlow<Set<String>>(emptySet())
    val tags: StateFlow<Set<String>> = _tags.asStateFlow()

    init {
        loadTags()
    }

    internal val nodesFlow = flow {
        val cellAvailable = isCellAvailable().fold({ false }, { it })
        emitAll(if (cellAvailable) buildNodesFlow() else flowOf(emptyData))
    }

    private fun buildNodesFlow() = combine(
        searchQueryFlow
            .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
            .onStart { emit("") }
            .distinctUntilChanged(),
        selectedTags
    ) { query, currentTags -> query to currentTags }
        .flatMapLatest { (query, currentTags) ->
            combine(
                getCellFilesPaged(
                    conversationId = navArgs.conversationId,
                    query = query,
                    onlyDeleted = navArgs.isRecycleBin ?: false,
                    tags = currentTags.toList(),
                ).cachedIn(viewModelScope),
                removedItemsFlow,
                downloadDataFlow
            ) { pagingData, removedItems, downloadData ->
                pagingData
                    .filter {
                        it.uuid !in removedItems
                    }
                    .map {
                        when (it) {
                            is Node.Folder -> it.toUiModel().copy(
                                downloadProgress = downloadData[it.uuid]?.progress
                            )

                            is Node.File -> {
                                it.toUiModel().copy(
                                    downloadProgress = downloadData[it.uuid]?.progress,
                                    localPath = downloadData[it.uuid]?.localPath?.toString()
                                )
                            }
                        }
                    }
            }
        }
    fun updateSelectedTags(tags: Set<String>) {
        _selectedTags.value = tags
    }

    internal fun onSearchQueryUpdated(text: String) = viewModelScope.launch {
        searchQueryFlow.emit(text)
    }

    internal fun hasSearchQuery(): Boolean {
        return searchQueryFlow.value.isNotEmpty()
    }

    internal fun sendIntent(intent: CellViewIntent) {
        when (intent) {
            is CellViewIntent.OnFileClick -> onFileClick(intent.file)
            is CellViewIntent.OnItemMenuClick -> onItemMenuClick(intent.cellNode)
            is CellViewIntent.OnMenuItemActionSelected -> onMenuItemAction(intent.node, intent.action)
            is CellViewIntent.OnFileDownloadConfirmed -> downloadNode(intent.file)
            is CellViewIntent.OnNodeDeleteConfirmed -> deleteFile(intent.node)
            is CellViewIntent.OnNodeRestoreConfirmed -> restoreNodeFromRecycleBin(intent.node)
            is CellViewIntent.OnDownloadMenuClosed -> onDownloadMenuClosed()
        }
    }

    internal fun currentNodeUuid(): String? = navArgs.conversationId
    internal fun isRecycleBin(): Boolean = navArgs.isRecycleBin ?: false
    private fun isSearching(): Boolean = searchQueryFlow.value.isNotEmpty()
    private fun isConversationFiles(): Boolean = navArgs.conversationId != null && !isRecycleBin()
    private fun isAllFiles(): Boolean = navArgs.conversationId == null && !isRecycleBin()

    internal fun screenTitle(): String? = navArgs.screenTitle
    internal fun breadcrumbs(): Array<String>? = navArgs.breadcrumbs

    private fun onFileClick(cellNode: CellNodeUi.File) {
        when {
            cellNode.localFileAvailable() -> openLocalFile(cellNode)
            cellNode.canOpenWithUrl() -> openFileContentUrl(cellNode)
            else -> viewModelScope.launch { _downloadFileSheet.emit(cellNode) }
        }
    }

    private fun downloadNode(node: CellNodeUi) = viewModelScope.launch {

        val (nodeName, nodeRemotePath) = when (node) {
            is CellNodeUi.File -> Pair(node.name, node.remotePath)
            is CellNodeUi.Folder -> Pair(node.name + ZIP_EXTENSION, node.remotePath + ZIP_EXTENSION)
        }

        if (nodeName.isNullOrBlank()) {
            sendAction(ShowError(CellError.OTHER_ERROR))
            return@launch
        }

        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(publicDir, nodeName).toPath().toOkioPath()

        deleteIfExists(filePath)

        download(
            assetId = node.uuid,
            outFilePath = filePath,
            remoteFilePath = nodeRemotePath,
            assetSize = node.size ?: 0,
        ) { progress ->
            node.size?.let {
                updateDownloadProgress(progress, it, node, filePath)
            }
        }.onSuccess {
            updateDownloadData(node.uuid) {
                DownloadData(
                    localPath = filePath
                )
            }
        }.onFailure {
            _downloadFileSheet.update { null }
            sendAction(ShowError(CellError.DOWNLOAD_FAILED))
        }
    }

    private fun deleteIfExists(filePath: Path) = runCatching {
        if (kaliumFileSystem.exists(filePath)) {
            kaliumFileSystem.delete(filePath)
        }
    }

    private fun updateDownloadProgress(progress: Long, it: Long, node: CellNodeUi, path: Path) = viewModelScope.launch {

        val value = progress.toFloat() / it

        if (value < 1) {
            updateDownloadData(node.uuid) {
                DownloadData(
                    progress = value
                )
            }
        } else {
            updateDownloadData(node.uuid) {
                DownloadData(
                    localPath = path
                )
            }

            if (_downloadFileSheet.value?.uuid == node.uuid) {
                _downloadFileSheet.update { null }
                if (node is CellNodeUi.File) {
                    openLocalFile(node.copy(localPath = path.toString()))
                }
            }
        }
    }

    private fun openFileContentUrl(file: CellNodeUi.File) {
        file.contentUrl?.let { url ->
            fileHelper.openAssetUrlWithExternalApp(
                url = url,
                mimeType = file.mimeType,
                onError = {
                    sendAction(ShowError(CellError.NO_APP_FOUND))
                }
            )
        }
    }

    private fun openLocalFile(file: CellNodeUi.File) {
        file.localPath?.let { path ->
            fileHelper.openAssetFileWithExternalApp(
                localPath = path.toPath(),
                assetName = file.name,
                mimeType = file.mimeType,
                onError = {
                    sendAction(ShowError(CellError.NO_APP_FOUND))
                }
            )
        }
    }

    private fun onItemMenuClick(cellNode: CellNodeUi) = viewModelScope.launch {
        val list = when {
            isRecycleBin() -> {
                buildList {
                    add(NodeBottomSheetAction.RESTORE)
                    add(NodeBottomSheetAction.DELETE_PERMANENTLY)
                }
            }

            isConversationFiles() -> {
                buildList {
                    if (cellNode is CellNodeUi.File && cellNode.localFileAvailable()) {
                        add(NodeBottomSheetAction.SHARE)
                    }
                    add(NodeBottomSheetAction.PUBLIC_LINK)
                    add(NodeBottomSheetAction.DOWNLOAD)
                    add(NodeBottomSheetAction.ADD_REMOVE_TAGS)
                    add(NodeBottomSheetAction.MOVE)
                    add(NodeBottomSheetAction.RENAME)
                    add(NodeBottomSheetAction.DELETE)
                }
            }

            isAllFiles() || isSearching() -> {
                buildList {
                    if (cellNode is CellNodeUi.File && cellNode.localFileAvailable()) {
                        add(NodeBottomSheetAction.SHARE)
                    }
                    add(NodeBottomSheetAction.PUBLIC_LINK)
                    add(NodeBottomSheetAction.DOWNLOAD)
                }
            }

            else -> {
                emptyList()
            }
        }

        val menuOption = MenuOptions(
            node = cellNode,
            actions = list,
        )
        _menu.emit(menuOption)
    }

    private fun onMenuItemAction(node: CellNodeUi, action: NodeBottomSheetAction) {
        when (action) {
            NodeBottomSheetAction.SHARE -> {
                if (node is CellNodeUi.File) {
                    shareFile(node)
                } else {
                    sendAction(ShowPublicLinkScreen(node))
                }
            }

            NodeBottomSheetAction.PUBLIC_LINK -> sendAction(ShowPublicLinkScreen(node))
            NodeBottomSheetAction.ADD_REMOVE_TAGS -> sendAction(ShowAddRemoveTagsScreen(node))
            NodeBottomSheetAction.MOVE -> navArgs.conversationId?.let {
                sendAction(
                    ShowMoveToFolderScreen(
                        currentPath = it.substringBefore("/"),
                        nodeToMovePath = "$it/${node.name}",
                        uuid = node.uuid
                    )
                )
            }

            NodeBottomSheetAction.RENAME -> sendAction(ShowRenameScreen(node))
            NodeBottomSheetAction.DOWNLOAD -> downloadNode(node)
            NodeBottomSheetAction.RESTORE -> sendAction(ShowRestoreConfirmation(node = node))
            NodeBottomSheetAction.DELETE -> sendAction(ShowDeleteConfirmation(node = node, isPermanentDelete = false))
            NodeBottomSheetAction.DELETE_PERMANENTLY -> sendAction(ShowDeleteConfirmation(node = node, isPermanentDelete = true))
        }
    }

    private fun shareFile(cell: CellNodeUi.File) {
        cell.localPath?.let { localPath ->
            fileHelper.shareFileChooser(
                assetDataPath = localPath.toPath(),
                assetName = cell.name,
                mimeType = cell.mimeType,
                onError = { sendAction(ShowError(CellError.OTHER_ERROR)) }
            )
        } ?: run {
            sendAction(ShowError(CellError.OTHER_ERROR))
        }
    }

    private fun deleteFile(node: CellNodeUi) = viewModelScope.launch {

        removedItemsFlow.update {
            it + node.uuid
        }
        val localPath = if (node is CellNodeUi.File) {
            node.localPath
        } else {
            null
        }

        deleteCellAsset(node.uuid, localPath)
            .onFailure {
                sendAction(ShowError(CellError.OTHER_ERROR))
                removedItemsFlow.update {
                    it - node.uuid
                }
            }
    }

    private fun restoreNodeFromRecycleBin(node: CellNodeUi) {
        viewModelScope.launch {
            isLoading.value = true
            node.remotePath?.let {
                restoreNodeFromRecycleBinUseCase(it)
                    .onSuccess {
                        removedItemsFlow.update { deletedItems ->
                            deletedItems - node.uuid
                        }
                        isLoading.value = false
                        sendAction(RefreshData)
                    }
                    .onFailure {
                        isLoading.value = false
                        sendAction(ShowError(CellError.OTHER_ERROR))
                    }
            }
        }
    }

    private fun onDownloadMenuClosed() {
        _downloadFileSheet.update { null }
    }

    private fun updateDownloadData(uuid: String, block: () -> DownloadData) {
        downloadDataFlow.update { map ->
            val progressMap = map.toMutableMap()
            progressMap[uuid] = block()
            progressMap.toImmutableMap()
        }
    }

    fun loadTags() = viewModelScope.launch {
        getAllTagsUseCase().onSuccess { updated -> _tags.update { updated } }
    }

    companion object {
        const val ZIP_EXTENSION = ".zip"

        private val emptyData: PagingData<CellNodeUi> = PagingData.empty(
            LoadStates(
                refresh = LoadState.NotLoading(true),
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true)
            )
        )
    }
}

sealed interface CellViewIntent {
    data class OnFileClick(val file: CellNodeUi.File) : CellViewIntent
    data class OnItemMenuClick(val cellNode: CellNodeUi) : CellViewIntent
    data class OnMenuItemActionSelected(val node: CellNodeUi, val action: NodeBottomSheetAction) : CellViewIntent
    data class OnFileDownloadConfirmed(val file: CellNodeUi.File) : CellViewIntent
    data class OnNodeDeleteConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnNodeRestoreConfirmed(val node: CellNodeUi) : CellViewIntent
    data object OnDownloadMenuClosed : CellViewIntent
}

sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val node: CellNodeUi, val isPermanentDelete: Boolean) : CellViewAction
internal data class ShowRestoreConfirmation(val node: CellNodeUi) : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowRenameScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowAddRemoveTagsScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowMoveToFolderScreen(val currentPath: String, val nodeToMovePath: String, val uuid: String) : CellViewAction
internal data object RefreshData : CellViewAction

internal enum class CellError(val message: Int) {
    DOWNLOAD_FAILED(R.string.cell_files_download_failure_message),
    NO_APP_FOUND(R.string.no_app_found),
    OTHER_ERROR(R.string.action_failed)
}

data class MenuOptions(
    val node: CellNodeUi,
    val actions: List<NodeBottomSheetAction>
)

private data class DownloadData(
    val progress: Float? = null,
    val localPath: Path? = null,
)
