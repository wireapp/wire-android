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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.model.CellFileUi
import com.wire.android.feature.cells.ui.model.FileAction
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetCellFilesUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val getCellFiles: GetCellFilesUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val kaliumFileSystem: KaliumFileSystem,
) : SavedStateViewModel(savedStateHandle) {

    private val navArgs: CellFilesNavArgs = savedStateHandle.navArgs()

    private val _state: MutableStateFlow<CellViewState> = MutableStateFlow(CellViewState.Loading)
    internal val state = _state.asStateFlow()

    // Show menu with actions for the selected file.
    private val _menu: MutableSharedFlow<MenuOptions> = MutableSharedFlow()
    internal val menu = _menu.asSharedFlow()

    // Show bottom sheet with download progress.
    private val _downloadFile: MutableStateFlow<CellFileUi?> = MutableStateFlow(null)
    internal val downloadFile = _downloadFile.asStateFlow()

    private val _actions = Channel<CellViewAction>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    internal val actions = _actions.receiveAsFlow()

    // Download progress value for each file being downloaded.
    private val uploadProgress = mutableStateMapOf<String, Float>()

    private var searchQuery: String = ""

    private fun loadFiles(clearList: Boolean = false, pullToRefresh: Boolean = false) {

        _state.update { currentState ->
            when {
                clearList -> CellViewState.Loading

                pullToRefresh -> if (currentState is CellViewState.Files) {
                    currentState.copy(refreshing = searchQuery.isEmpty())
                } else {
                    CellViewState.Loading
                }

                else -> currentState
            }
        }

        viewModelScope.launch {
            getCellFiles.invoke(navArgs.conversationId, searchQuery)
                .onSuccess { nodes ->
                    _state.update {
                        if (nodes.isEmpty()) {
                            CellViewState.Empty(isSearchResult = searchQuery.isNotEmpty())
                        } else {
                            CellViewState.Files(
                                files = nodes.map { it.toUiModel(uploadProgress[it.uuid]) },
                                refreshing = false,
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { currentState ->
                        when (currentState) {
                            is CellViewState.Files -> {
                                sendAction(ShowError(CellError.LOAD_FAILED))
                                currentState.copy(refreshing = false)
                            }
                            else -> CellViewState.Error
                        }
                    }
                }
        }
    }

    internal fun onSearchQueryUpdated(text: String) {
        if (searchQuery != text) {
            searchQuery = text
            loadFiles()
        }
    }

    internal fun sendIntent(intent: CellViewIntent) {
        when (intent) {
            is CellViewIntent.LoadFiles -> loadFiles(intent.clearList, intent.pullToRefresh)
            is CellViewIntent.OnFileClick -> onFileClick(intent.file)
            is CellViewIntent.OnFileMenuClick -> onFileMenuClick(intent.file)
            is CellViewIntent.OnMenuActionSelected -> onMenuAction(intent.file, intent.action)
            is CellViewIntent.OnFileDownloadConfirmed -> downloadFile(intent.file)
            is CellViewIntent.OnFileDeleteConfirmed -> deleteFile(intent.file)
            is CellViewIntent.OnDownloadMenuClosed -> onDownloadMenuClosed()
        }
    }

    private fun onFileClick(file: CellFileUi) {
        when {
            file.localFileAvailable() -> openLocalFile(file)
            file.canOpenWithUrl() -> openFileContentUrl(file)
            else -> viewModelScope.launch { _downloadFile.emit(file) }
        }
    }

    private fun downloadFile(file: CellFileUi) = viewModelScope.launch {

        val path = kaliumFileSystem.providePersistentAssetPath(
            file.fileName ?: run {
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
            updateFile(file.uuid) {
                copy(localPath = path.toString())
            }
        }.onFailure {
            _downloadFile.update { null }
            sendAction(ShowError(CellError.DOWNLOAD_FAILED))
        }
    }

    private fun updateDownloadProgress(progress: Long, it: Long, file: CellFileUi, path: Path) {

        val value = progress.toFloat() / it

        if (value < 1) {
            uploadProgress[file.uuid] = value
            updateFile(file.uuid) { copy(downloadProgress = value) }

            if (_downloadFile.value?.uuid == file.uuid) {
                _downloadFile.update { file.copy(downloadProgress = value) }
            }
        } else {
            uploadProgress.remove(file.uuid)

            updateFile(file.uuid) {
                copy(downloadProgress = null)
            }

            if (_downloadFile.value?.uuid == file.uuid) {
                _downloadFile.update { null }
                openLocalFile(file.copy(localPath = path.toString()))
            }
        }
    }

    private fun openFileContentUrl(file: CellFileUi) {
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

    private fun openLocalFile(file: CellFileUi) {
        file.localPath?.let { path ->
            fileHelper.openAssetFileWithExternalApp(
                localPath = path.toPath(),
                assetName = file.fileName,
                mimeType = file.mimeType,
                onError = {
                    sendAction(ShowError(CellError.NO_APP_FOUND))
                }
            )
        }
    }

    private fun onFileMenuClick(file: CellFileUi) = viewModelScope.launch {
        _menu.emit(
            MenuOptions(
                file = file,
                actions = buildList {
                    if (!file.localFileAvailable()) {
                        add(FileAction.SAVE)
                    } else {
                        add(FileAction.SHARE)
                    }
                    add(FileAction.PUBLIC_LINK)
                    add(FileAction.DELETE)
                },
            )
        )
    }

    private fun onMenuAction(file: CellFileUi, action: FileAction) {
        when (action) {
            FileAction.SAVE -> downloadFile(file)
            FileAction.SHARE -> shareFile(file)
            FileAction.PUBLIC_LINK -> sendAction(ShowPublicLinkScreen(file))
            FileAction.DELETE -> sendAction(ShowDeleteConfirmation(file))
        }
    }

    private fun shareFile(file: CellFileUi) {
        file.localPath?.let { localPath ->
            fileHelper.shareFileChooser(
                assetDataPath = localPath.toPath(),
                assetName = file.fileName,
                mimeType = file.mimeType,
                onError = { sendAction(ShowError(CellError.OTHER_ERROR)) }
            )
        } ?: run {
            sendAction(ShowError(CellError.OTHER_ERROR))
        }
    }

    private fun deleteFile(file: CellFileUi) = viewModelScope.launch {

        removeFile(file.uuid)

        deleteCellAsset(file.uuid, file.localPath)
            .onFailure {
                sendAction(ShowError(CellError.OTHER_ERROR))
                loadFiles()
            }
    }

    private fun updateFile(id: String, block: CellFileUi.() -> CellFileUi) {
        _state.update {
            if (it is CellViewState.Files) {
                it.copy(
                    files = it.files.map { file ->
                        if (file.uuid == id) {
                            file.block()
                        } else {
                            file
                        }
                    }
                )
            } else {
                it
            }
        }
    }

    private fun removeFile(id: String) {
        _state.update { currentState ->
            if (currentState is CellViewState.Files) {
                currentState.copy(files = currentState.files.filter { it.uuid != id }).let { newState ->
                    if (newState.files.isEmpty()) {
                        CellViewState.Empty(false)
                    } else {
                        newState
                    }
                }
            } else {
                currentState
            }
        }
    }

    private fun onDownloadMenuClosed() {
        _downloadFile.update { null }
    }

    private fun sendAction(action: CellViewAction) {
        viewModelScope.launch { _actions.send(action) }
    }
}

internal sealed interface CellViewIntent {
    data class LoadFiles(val clearList: Boolean = false, val pullToRefresh: Boolean = false) : CellViewIntent
    data class OnFileClick(val file: CellFileUi) : CellViewIntent
    data class OnFileMenuClick(val file: CellFileUi) : CellViewIntent
    data class OnMenuActionSelected(val file: CellFileUi, val action: FileAction) : CellViewIntent
    data class OnFileDownloadConfirmed(val file: CellFileUi) : CellViewIntent
    data class OnFileDeleteConfirmed(val file: CellFileUi) : CellViewIntent
    data object OnDownloadMenuClosed : CellViewIntent
}

internal sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val file: CellFileUi) : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val file: CellFileUi) : CellViewAction

internal enum class CellError(val message: Int) {
    LOAD_FAILED(R.string.cell_files_load_failure_message),
    DOWNLOAD_FAILED(R.string.cell_files_download_failure_message),
    NO_APP_FOUND(R.string.no_app_found),
    OTHER_ERROR(R.string.action_failed)
}

@Immutable
internal sealed interface CellViewState {
    data object Loading : CellViewState
    data class Empty(val isSearchResult: Boolean) : CellViewState
    data object Error : CellViewState
    data class Files(
        val files: List<CellFileUi> = emptyList(),
        val refreshing: Boolean = false,
    ) : CellViewState
}

internal data class MenuOptions(
    val file: CellFileUi,
    val actions: List<FileAction>
)
