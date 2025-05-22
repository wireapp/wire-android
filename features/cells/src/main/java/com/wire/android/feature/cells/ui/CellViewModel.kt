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
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.model.BottomSheetAction
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.FileAction
import com.wire.android.feature.cells.ui.model.FolderAction
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class CellViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val kaliumFileSystem: KaliumFileSystem,
    @ApplicationContext val context: Context
) : SavedStateViewModel(savedStateHandle) {

    private val navArgs: CellFilesNavArgs = savedStateHandle.navArgs()

    // Show menu with actions for the selected file.
    private val _menu: MutableSharedFlow<MenuOptions> = MutableSharedFlow()
    internal val menu = _menu.asSharedFlow()

    // Show bottom sheet with download progress.
    private val _downloadFileSheet: MutableStateFlow<CellNodeUi.File?> = MutableStateFlow(null)
    internal val downloadFileSheet = _downloadFileSheet.asStateFlow()

    private val _actions = Channel<CellViewAction>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val actions = _actions
        .receiveAsFlow()
        .flowOn(Dispatchers.Main.immediate)

    // Download progress value for each file being downloaded.
    private val downloadDataFlow = MutableStateFlow<Map<String, DownloadData>>(emptyMap())

    private val searchQueryFlow: MutableStateFlow<String> = MutableStateFlow("")

    private val removedItemsFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    internal val nodesFlow = searchQueryFlow
        .debounce { if (it.isEmpty()) 0L else DEFAULT_SEARCH_QUERY_DEBOUNCE }
        .onStart { emit("") }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            combine(
                getCellFilesPaged(navArgs.conversationId, query, navArgs.onlyDeleted ?: false).cachedIn(viewModelScope),
                removedItemsFlow,
                downloadDataFlow,
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
            is CellViewIntent.OnMenuFileActionSelected -> onMenuFileAction(intent.file, intent.action)
            is CellViewIntent.OnMenuFolderActionSelected -> onMenuFolderAction(intent.folder, intent.action)
            is CellViewIntent.OnFileDownloadConfirmed -> downloadNode(intent.file)
            is CellViewIntent.OnNodeDeleteConfirmed -> deleteFile(intent.file)
            is CellViewIntent.OnDownloadMenuClosed -> onDownloadMenuClosed()
        }
    }

    internal fun currentNodeUuid(): String? = navArgs.conversationId
    internal fun isRecycleBin(): Boolean = navArgs.onlyDeleted ?: false
    internal fun screenTitle(): String? = navArgs.screenTitle

    private fun onFileClick(cellNode: CellNodeUi.File) {
        when {
            cellNode.localFileAvailable() -> openLocalFile(cellNode)
            cellNode.canOpenWithUrl() -> openFileContentUrl(cellNode)
            else -> viewModelScope.launch { _downloadFileSheet.emit(cellNode) }
        }
    }

    private fun downloadNode(node: CellNodeUi) = viewModelScope.launch {

        if (node.name.isNullOrBlank()) {
            sendAction(ShowError(CellError.OTHER_ERROR))
            return@launch
        }
//
        val (nodeName, nodeRemotePath) = when (node) {
            is CellNodeUi.File -> Pair(node.name, node.remotePath)
            is CellNodeUi.Folder -> Pair(node.name + ZIP_EXTENSION, node.remotePath + ZIP_EXTENSION)
        }

        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(publicDir, nodeName).toPath().toOkioPath()

        if (kaliumFileSystem.exists(filePath)) {
            kaliumFileSystem.delete(filePath)
        }

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
        val menuOption = when (cellNode) {
            is CellNodeUi.File -> {
                val list = buildList {
                    if (isRecycleBin()) {
                        add(BottomSheetAction.File(FileAction.RESTORE))
                        add(BottomSheetAction.File(FileAction.DELETE_PERMANENTLY))
                    } else {
                        if (!cellNode.localFileAvailable()) {
                            add(BottomSheetAction.File(FileAction.SAVE))
                        } else {
                            add(BottomSheetAction.File(FileAction.SHARE))
                        }
                        add(BottomSheetAction.File(FileAction.PUBLIC_LINK))
                        add(BottomSheetAction.File(FileAction.MOVE))
                        add(BottomSheetAction.File(FileAction.DELETE))
                    }
                }
                MenuOptions.FileMenuOptions(
                    cellNodeUi = cellNode,
                    actions = list,
                )
            }

            is CellNodeUi.Folder -> {
                val list = buildList {
                    if (isRecycleBin()) {
                        add(BottomSheetAction.Folder(FolderAction.RESTORE))
                        add(BottomSheetAction.Folder(FolderAction.DELETE_PERMANENTLY))
                    } else {
                        add(BottomSheetAction.Folder(FolderAction.SHARE))
                        add(BottomSheetAction.Folder(FolderAction.MOVE))
                        add(BottomSheetAction.Folder(FolderAction.DOWNLOAD))
                        add(BottomSheetAction.Folder(FolderAction.DELETE))
                    }
                }
                MenuOptions.FolderMenuOptions(
                    cellNodeUi = cellNode,
                    actions = list,
                )
            }
        }

        _menu.emit(menuOption)
    }

    private fun onMenuFileAction(file: CellNodeUi.File, action: BottomSheetAction.File) {
        when (action.action) {
            FileAction.SAVE -> downloadNode(file)
            FileAction.SHARE -> shareFile(file)
            FileAction.PUBLIC_LINK -> sendAction(ShowPublicLinkScreen(file))
            FileAction.DELETE -> sendAction(ShowDeleteConfirmation(node = file, isPermanentDelete = false))
            FileAction.DELETE_PERMANENTLY -> sendAction(ShowDeleteConfirmation(node = file, isPermanentDelete = true))
            FileAction.MOVE -> navArgs.conversationId?.let {
                sendAction(
                    ShowMoveToFolderScreen(
                        currentPath = it.substringBefore("/"),
                        nodeToMovePath = "$it/${file.name}",
                        uuid = file.uuid
                    )
                )
            }

            FileAction.RESTORE -> restoreNodeFromRecycleBin(file.remotePath)
        }
    }

    private fun onMenuFolderAction(folder: CellNodeUi.Folder, action: BottomSheetAction.Folder) {
        when (action.action) {
            FolderAction.SHARE -> sendAction(ShowPublicLinkScreen(folder))
            FolderAction.DOWNLOAD -> downloadNode(folder)
            FolderAction.MOVE -> navArgs.conversationId?.let {
                sendAction(
                    ShowMoveToFolderScreen(
                        currentPath = it.substringBefore("/"),
                        nodeToMovePath = "$it/${folder.name}",
                        uuid = folder.uuid
                    )
                )
            }
            FolderAction.DELETE -> sendAction(ShowDeleteConfirmation(node = folder, isPermanentDelete = false))
            FolderAction.DELETE_PERMANENTLY -> sendAction(ShowDeleteConfirmation(node = folder, isPermanentDelete = true))
            FolderAction.RESTORE -> restoreNodeFromRecycleBin(folder.remotePath)
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

    fun restoreNodeFromRecycleBin(path: String?) {
        viewModelScope.launch {
            path?.let {
                restoreNodeFromRecycleBinUseCase(path)
                    .onSuccess {
                        sendAction(RefreshData)
                    }
                    .onFailure {
                        sendAction(ShowError(CellError.OTHER_ERROR))
                    }
            }
        }
    }

    private fun onDownloadMenuClosed() {
        _downloadFileSheet.update { null }
    }

    private fun sendAction(action: CellViewAction) {
        viewModelScope.launch { _actions.send(action) }
    }

    private fun updateDownloadData(uuid: String, block: () -> DownloadData) {
        downloadDataFlow.update { map ->
            val progressMap = map.toMutableMap()
            progressMap[uuid] = block()
            progressMap.toImmutableMap()
        }
    }

    companion object {
        const val ZIP_EXTENSION = ".zip"
    }
}

sealed interface CellViewIntent {
    data class OnFileClick(val file: CellNodeUi.File) : CellViewIntent
    data class OnItemMenuClick(val cellNode: CellNodeUi) : CellViewIntent
    data class OnMenuFileActionSelected(val file: CellNodeUi.File, val action: BottomSheetAction) : CellViewIntent
    data class OnMenuFolderActionSelected(val folder: CellNodeUi.Folder, val action: BottomSheetAction) : CellViewIntent
    data class OnFileDownloadConfirmed(val file: CellNodeUi.File) : CellViewIntent
    data class OnNodeDeleteConfirmed(val file: CellNodeUi) : CellViewIntent
    data object OnDownloadMenuClosed : CellViewIntent
}

sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val node: CellNodeUi, val isPermanentDelete: Boolean) : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowMoveToFolderScreen(val currentPath: String, val nodeToMovePath: String, val uuid: String) : CellViewAction
internal data object RefreshData : CellViewAction

internal enum class CellError(val message: Int) {
    DOWNLOAD_FAILED(R.string.cell_files_download_failure_message),
    NO_APP_FOUND(R.string.no_app_found),
    OTHER_ERROR(R.string.action_failed)
}

sealed class MenuOptions {
    data class FileMenuOptions(
        val cellNodeUi: CellNodeUi.File,
        val actions: List<BottomSheetAction.File>
    ) : MenuOptions()

    data class FolderMenuOptions(
        val cellNodeUi: CellNodeUi.Folder,
        val actions: List<BottomSheetAction.Folder>
    ) : MenuOptions()
}

private data class DownloadData(
    val progress: Float? = null,
    val localPath: Path? = null,
)
