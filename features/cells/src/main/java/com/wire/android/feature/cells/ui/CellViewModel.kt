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
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
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
import okio.Path.Companion.toPath
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class CellViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val kaliumFileSystem: KaliumFileSystem,
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
                getCellFilesPaged(navArgs.conversationId, query).cachedIn(viewModelScope),
                removedItemsFlow,
                downloadDataFlow,
            ) { pagingData, removedItems, downloadData ->
                pagingData
                    .filter {
                        it.uuid !in removedItems
                    }
                    .map {
                        when (it) {
                            is Node.Folder -> it.toUiModel()
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

    internal fun currentNodeUuid(): String? = navArgs.conversationId

    internal fun screenTitle(): String? = navArgs.screenTitle

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
            is CellViewIntent.OnFileDownloadConfirmed -> downloadFile(intent.file)
            is CellViewIntent.OnFileDeleteConfirmed -> deleteFile(intent.file)
            is CellViewIntent.OnDownloadMenuClosed -> onDownloadMenuClosed()
        }
    }

    private fun onFileClick(cellNode: CellNodeUi.File) {
        when {
            cellNode.localFileAvailable() -> openLocalFile(cellNode)
            cellNode.canOpenWithUrl() -> openFileContentUrl(cellNode)
            else -> viewModelScope.launch { _downloadFileSheet.emit(cellNode) }
        }
    }

    private fun downloadFile(file: CellNodeUi.File) = viewModelScope.launch {

        val path = kaliumFileSystem.providePersistentAssetPath(
            file.name ?: run {
                sendAction(ShowError(CellError.OTHER_ERROR))
                return@launch
            }
        )

        if (kaliumFileSystem.exists(path)) {
            kaliumFileSystem.delete(path)
        }

        download(
            assetId = file.uuid,
            outFilePath = path,
            remoteFilePath = file.remotePath,
            assetSize = file.assetSize ?: 0,
        ) { progress ->
            file.assetSize?.let {
                updateDownloadProgress(progress, it, file, path)
            }
        }.onSuccess {
            updateDownloadData(file.uuid) {
                DownloadData(
                    localPath = path
                )
            }
        }.onFailure {
            _downloadFileSheet.update { null }
            sendAction(ShowError(CellError.DOWNLOAD_FAILED))
        }
    }

    private fun updateDownloadProgress(progress: Long, it: Long, file: CellNodeUi.File, path: Path) = viewModelScope.launch {

        val value = progress.toFloat() / it

        if (value < 1) {
            updateDownloadData(file.uuid) {
                DownloadData(
                    progress = value
                )
            }
        } else {
            updateDownloadData(file.uuid) {
                DownloadData(
                    localPath = path
                )
            }

            if (_downloadFileSheet.value?.uuid == file.uuid) {
                _downloadFileSheet.update { null }
                openLocalFile(file.copy(localPath = path.toString()))
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
                    if (!cellNode.localFileAvailable()) {
                        add(BottomSheetAction.File(FileAction.SAVE))
                    } else {
                        add(BottomSheetAction.File(FileAction.SHARE))
                    }
                    add(BottomSheetAction.File(FileAction.PUBLIC_LINK))
                    add(BottomSheetAction.File(FileAction.DELETE))
                }
                MenuOptions.FileMenuOptions(
                    cellNodeUi = cellNode,
                    actions = list,
                )
            }

            is CellNodeUi.Folder -> {
                val list = buildList {
                    add(BottomSheetAction.Folder(FolderAction.SHARE))
                    add(BottomSheetAction.Folder(FolderAction.MOVE))
                    add(BottomSheetAction.Folder(FolderAction.DOWNLOAD))
                    add(BottomSheetAction.Folder(FolderAction.DELETE))
                }
                MenuOptions.FolderMenuOptions(
                    cellNodeUi = cellNode,
                    actions = list,
                )
            }
        }

        _menu.emit(menuOption)
    }

    private fun onMenuFileAction(file: CellNodeUi.File, action: BottomSheetAction) {
        when ((action as BottomSheetAction.File).action) {
            FileAction.SAVE -> downloadFile(file)
            FileAction.SHARE -> shareFile(file)
            FileAction.PUBLIC_LINK -> sendAction(ShowPublicLinkScreen(file))
            FileAction.DELETE -> sendAction(ShowDeleteConfirmation(file))
        }
    }

    private fun onMenuFolderAction(folder: CellNodeUi.Folder, action: BottomSheetAction) {
        when ((action as BottomSheetAction.Folder).action) {
            FolderAction.SHARE -> {
                sendAction(ShowPublicLinkScreen(folder))
            }
            FolderAction.MOVE -> TODO()
            FolderAction.DOWNLOAD -> TODO()
            FolderAction.DELETE -> TODO()
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

    private fun deleteFile(file: CellNodeUi.File) = viewModelScope.launch {

        removedItemsFlow.update {
            it + file.uuid
        }

        deleteCellAsset(file.uuid, file.localPath)
            .onFailure {
                sendAction(ShowError(CellError.OTHER_ERROR))
                removedItemsFlow.update {
                    it - file.uuid
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
}

sealed interface CellViewIntent {
    data class OnFileClick(val file: CellNodeUi.File) : CellViewIntent
    data class OnItemMenuClick(val cellNode: CellNodeUi) : CellViewIntent
    data class OnMenuFileActionSelected(val file: CellNodeUi.File, val action: BottomSheetAction) : CellViewIntent
    data class OnMenuFolderActionSelected(val folder: CellNodeUi.Folder, val action: BottomSheetAction) : CellViewIntent
    data class OnFileDownloadConfirmed(val file: CellNodeUi.File) : CellViewIntent
    data class OnFileDeleteConfirmed(val file: CellNodeUi.File) : CellViewIntent
    data object OnDownloadMenuClosed : CellViewIntent
}

sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val file: CellNodeUi.File) : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val cellNode: CellNodeUi) : CellViewAction
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
