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
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.ramcosta.composedestinations.generated.cells.destinations.ConversationFilesScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.SearchScreenDestination
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.data.FileFilters
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.common.functional.fold
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.featureConfig.CollaboraEdition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class CellViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val download: DownloadCellFileUseCase,
    private val isCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val fileHelper: FileHelper,
    private val fileNameResolver: FileNameResolver,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val cellFileActionsMenu: CellFileActionsMenu,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) : ActionsViewModel<CellViewAction>() {

    private val navArgs: CellFilesNavArgs = ConversationFilesScreenDestination.argsFrom(savedStateHandle)
    private val searchNavArgs: SearchNavArgs? = try {
        SearchScreenDestination.argsFrom(savedStateHandle)
    } catch (_: RuntimeException) {
        // Not coming from Search screen, ignore
        null
    }

    // Show menu with actions for the selected file.
    private val _menu: MutableSharedFlow<MenuOptions> = MutableSharedFlow()
    internal val menu = _menu.asSharedFlow()


    private val _isRestoreInProgress = MutableStateFlow(false)
    val isRestoreInProgress = _isRestoreInProgress.asStateFlow()

    private val _isDeleteInProgress = MutableStateFlow(false)
    val isDeleteInProgress = _isDeleteInProgress.asStateFlow()

    // Download progress value for each file being downloaded.
    private val downloadDataFlow = MutableStateFlow<Map<String, DownloadData>>(emptyMap())


    // Active open-download jobs keyed by node UUID, used to support cancellation of loading.
    private val openDownloadJobs = mutableMapOf<String, Job>()

    // Monotonically-increasing generation per UUID — incremented on every new startOpenDownload call.
    // Stale progress callbacks from a previous (cancelled) download carry an old generation and are ignored.
    private val openDownloadGeneration = mutableMapOf<String, Long>()

    // Open-loading state: tracks files being silently downloaded for immediate open.
    private val openLoadStateFlow = MutableStateFlow<Map<String, OpenLoadState>>(emptyMap())

    /** Public map of uuid → (isOpenLoading, isOpenReady) for screens that build their own paging flow (e.g. Search). */
    internal val openLoadStates: StateFlow<Map<String, OpenLoadState>> = openLoadStateFlow.asStateFlow()

    /**
     * File-ready events for the "ready to open" snackbar. Backed by the singleton channel so the
     * event reaches whichever screen is currently active, even if the download finished on a different
     * screen (e.g. completed in Search while the user navigated back to All Files).
     */
    internal val fileReadyFlow = sharedPathCache.fileReadyEvents

    /** Cached local file paths from completed open-downloads, keyed by uuid. Used by Search screen overlay. */
    private val _cachedLocalPaths = MutableStateFlow<Map<String, String>>(emptyMap())
    internal val cachedLocalPaths: StateFlow<Map<String, String>> = _cachedLocalPaths.asStateFlow()

    private val removedItemsFlow: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())

    // Used to navigate to the root of the recycle bin after restoring a parent folder.
    private val _navigateToRecycleBinRoot = MutableStateFlow(false)
    val navigateToRecycleBinRoot: StateFlow<Boolean> get() = _navigateToRecycleBinRoot

    private val _isPullToRefresh = MutableStateFlow(false)
    val isPullToRefresh: StateFlow<Boolean> = _isPullToRefresh.asStateFlow()

    private val _pagingRefreshDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val pagingRefreshDone: SharedFlow<Unit> = _pagingRefreshDone

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    private val cellAvailableFlow = MutableStateFlow(false)

    private var isCollaboraEnabled: Boolean = false

    init {
        loadWireCellConfig()
        checkCellAvailabilityAndRefresh()
        viewModelScope.launch {
            try {
                sharedPathCache.paths.collect { _cachedLocalPaths.value = it }
            } catch (_: Throwable) {
                // sharedPathCache.paths unavailable — cachedLocalPaths stays empty
            }
        }
    }

    private fun checkCellAvailabilityAndRefresh() = viewModelScope.launch {
        val cellAvailable = isCellAvailable().fold({ false }, { it })
        cellAvailableFlow.value = cellAvailable

        if (cellAvailable) {
            refreshNodes()
        }
    }

    private val sharedNodesFlow = cellAvailableFlow.flatMapLatest { cellAvailable ->
        if (!cellAvailable) {
            return@flatMapLatest flow {
                emit(emptyData)
            }
        }

        refreshTrigger.flatMapLatest {
            combine(
                getCellFilesPaged(
                    conversationId = navArgs.conversationId,
                    fileFilters = FileFilters(
                        onlyDeleted = navArgs.isRecycleBin ?: false,
                    ),
                ).cachedIn(viewModelScope),
                removedItemsFlow,
                downloadDataFlow,
                openLoadStateFlow,
                _cachedLocalPaths,
            ) { pagingData, removedItems, downloadData, openLoadStates, cachedPaths ->
                var emittedRefreshDone = false

                pagingData
                    .filter { node: Node -> node.uuid !in removedItems }
                    .map { node ->
                        if (!emittedRefreshDone) {
                            emittedRefreshDone = true

                            if (_isPullToRefresh.value) {
                                _isPullToRefresh.value = false
                            }

                            _pagingRefreshDone.tryEmit(Unit)
                        }

                        val openLoadState = openLoadStates[node.uuid]
                        when (node) {
                            is Node.Folder -> node.toUiModel().copy(
                                downloadProgress = downloadData[node.uuid]?.progress
                            )

                            is Node.File -> node.toUiModel().copy(
                                downloadProgress = downloadData[node.uuid]?.progress,
                                localPath = downloadData[node.uuid]?.localPath?.toString()
                                    ?: (openLoadState as? OpenLoadState.Ready)?.localPath?.toString()
                                    ?: cachedPaths[node.uuid]
                                    ?: node.localPath,
                                isOpenLoading = openLoadState is OpenLoadState.Loading,
                                isOpenReady = openLoadState is OpenLoadState.Ready,
                                isOpenError = openLoadState is OpenLoadState.Error,
                                openLoadProgress = (openLoadState as? OpenLoadState.Loading)?.progress,
                            )
                        }
                    }
            }
        }
    }.shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 1)

    internal val nodesFlow = cellAvailableFlow.flatMapLatest { cellAvailable ->
        if (!cellAvailable || searchNavArgs != null) {
            flowOf(emptyData)
        } else {
            sharedNodesFlow
        }
    }

    fun onPullToRefresh() {
        _isPullToRefresh.value = true
        refreshNodes()
    }

    private fun refreshNodes() {
        viewModelScope.launch {
            refreshTrigger.tryEmit(Unit)
        }
    }

    internal fun sendIntent(intent: CellViewIntent) {
        when (intent) {
            is CellViewIntent.OnItemClick -> when (intent.file) {
                is CellNodeUi.File -> onFileClick(intent.file)
                is CellNodeUi.Folder -> onFolderClick(intent.file)
            }

            is CellViewIntent.OnItemMenuClick -> onItemMenuClick(intent.cellNode)
            is CellViewIntent.OnMenuItemActionSelected -> onMenuItemAction(intent.node, intent.action)
            is CellViewIntent.OnNodeDeleteConfirmed -> deleteFile(intent.node)
            is CellViewIntent.OnNodeRestoreConfirmed -> restoreNodeFromRecycleBin(intent.node)
            is CellViewIntent.OnParentFolderRestoreConfirmed -> restoreNodeFromRecycleBin(intent.node)
            is CellViewIntent.OnCancelDownload -> cancelDownload(intent.uuid)
            is CellViewIntent.OnScreenLeave -> clearAllErrorStates()
        }
    }

    internal fun currentNodeUuid(): String? = navArgs.conversationId
    internal fun isRecycleBin(): Boolean = navArgs.isRecycleBin ?: false
    private fun isConversationFiles(): Boolean = navArgs.conversationId != null && !isRecycleBin()
    private fun isAllFiles(): Boolean = navArgs.conversationId == null && !isRecycleBin()

    internal fun screenTitle(): String? = navArgs.screenTitle
    internal fun breadcrumbs(): Array<String>? = navArgs.breadcrumbs

    private fun onFileClick(cellNode: CellNodeUi.File) {
        when {
            cellNode.isOpenReady -> openLocalFile(cellNode)
            cellNode.isOpenLoading -> cancelOpenDownload(cellNode.uuid)
            cellNode.isOpenError -> startOpenDownload(cellNode)
            cellNode.localFileAvailable() -> openLocalFile(cellNode)
            cellNode.canOpenWithUrl() -> openFileContentUrl(cellNode)
            else -> startOpenDownload(cellNode)
        }
    }

    private fun startOpenDownload(cellNode: CellNodeUi.File) {
        // Stamp a new generation for this download session.
        val myGeneration = (openDownloadGeneration[cellNode.uuid] ?: 0L) + 1L
        openDownloadGeneration[cellNode.uuid] = myGeneration

        val job = viewModelScope.launch {
            val nodeName = cellNode.name ?: run {
                sendAction(ShowError(CellError.OTHER_ERROR))
                return@launch
            }

            val cacheDir = fileHelper.getCacheDir()
            val filePath = fileNameResolver.getUniqueFile(cacheDir, nodeName).toPath().toOkioPath()

            // Track whether the 300ms threshold was crossed before download completed
            var spinnerShown = false

            val showSpinnerJob = launch {
                delay(OPEN_SPINNER_DELAY_MS)
                spinnerShown = true
                updateOpenLoadState(cellNode.uuid) { OpenLoadState.Loading() }
            }

            download(
                assetId = cellNode.uuid,
                outFilePath = filePath,
                remoteFilePath = cellNode.remotePath,
                assetSize = cellNode.size ?: 0,
            ) { progress ->
                // Dispatch to main thread. Guard with generation check so stale callbacks
                // from a cancelled download never overwrite state belonging to a newer session.
                viewModelScope.launch {
                    if (openDownloadGeneration[cellNode.uuid] == myGeneration) {
                        val assetSize = cellNode.size ?: 0
                        if (assetSize > 0) {
                            val progressValue = (progress.toFloat() / assetSize).coerceIn(0f, 1f)
                            updateOpenLoadState(cellNode.uuid) { OpenLoadState.Loading(progressValue) }
                        }
                    }
                }
            }
                .onSuccess {
                    showSpinnerJob.cancel()
                    openDownloadJobs.remove(cellNode.uuid)
                    openDownloadGeneration[cellNode.uuid] = (openDownloadGeneration[cellNode.uuid] ?: 0L) + 1L
                    // Cache the local path so future taps open directly
                    updateDownloadData(cellNode.uuid) { DownloadData(localPath = filePath) }
                    if (!spinnerShown) {
                        // Fast path: download completed before 300ms threshold — open instantly
                        clearOpenLoadState(cellNode.uuid)
                        openLocalFile(cellNode.copy(localPath = filePath.toString()))
                    } else {
                        // Slow path: spinner was already shown — show ready state + snackbar
                        updateOpenLoadState(cellNode.uuid) { OpenLoadState.Ready(filePath) }
                        sharedPathCache.emitFileReady(cellNode.copy(localPath = filePath.toString()))
                        // Auto-dismiss the "Ready" state after 3 seconds
                        launch {
                            delay(OPEN_READY_DISMISS_MS)
                            clearOpenLoadState(cellNode.uuid)
                        }
                    }
                }
                .onFailure {
                    showSpinnerJob.cancel()
                    openDownloadJobs.remove(cellNode.uuid)
                    openDownloadGeneration[cellNode.uuid] = (openDownloadGeneration[cellNode.uuid] ?: 0L) + 1L
                    updateOpenLoadState(cellNode.uuid) { OpenLoadState.Error }
                }
        }
        openDownloadJobs[cellNode.uuid] = job
    }

    internal fun cancelOpenDownload(uuid: String) {
        openDownloadJobs.remove(uuid)?.cancel()
        // Increment instead of removing so that any already-dispatched viewModelScope.launch
        // callbacks from the cancelled download (which escape job cancellation) never match
        // the generation of the next download session.
        openDownloadGeneration[uuid] = (openDownloadGeneration[uuid] ?: 0L) + 1L
        clearOpenLoadState(uuid)
    }

    private fun onFolderClick(cellNode: CellNodeUi.Folder) {
        val path = when {
            isRecycleBin() -> if (currentNodeUuid()?.contains("recycle_bin") == true) {
                "${currentNodeUuid()}/${cellNode.name}"
            } else {
                "${currentNodeUuid()}/recycle_bin/${cellNode.name}"
            }

            isConversationFiles() -> "${currentNodeUuid()}/${cellNode.name}"
            else -> cellNode.remotePath
        } ?: run {
            sendAction(ShowError(CellError.OTHER_ERROR))
            return
        }

        // UUID of the top parent folder for Recycle Bin items, used when restoring a folder
        val parentFolderUuid = if (isRecycleBin()) {
            navArgs.parentFolderUuid ?: cellNode.uuid
        } else {
            null
        }

        sendAction(
            OpenFolder(
                path = path,
                title = cellNode.name ?: "",
                parentFolderUuid = parentFolderUuid,
            )
        )
    }

    internal fun cancelDownload(uuid: String) {
        cancelOpenDownload(uuid)
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

        val menuItems = cellFileActionsMenu.buildMenu(
            cellNode = cellNode,
            isRecycleBin = isRecycleBin(),
            isConversationFiles = isConversationFiles(),
            isAllFiles = isAllFiles(),
            isSearching = searchNavArgs?.screenType == DriveSearchScreenType.SHARED_DRIVE ||
                    searchNavArgs?.screenType == DriveSearchScreenType.DRIVE,
            isCollaboraEnabled = isCollaboraEnabled,
        )

        _menu.emit(MenuOptions(cellNode, menuItems))
    }

    private fun onMenuItemAction(node: CellNodeUi, action: NodeBottomSheetAction) {
        cellFileActionsMenu.onMenuItemAction(
            conversationId = navArgs.conversationId,
            parentFolderUuid = navArgs.parentFolderUuid,
            node = node,
            action = action,
        ) { result ->
            when (result) {
                is CellFileActionsMenu.Action -> sendAction(result.action)
                is CellFileActionsMenu.Edit -> editNode(result.node.uuid)
                is CellFileActionsMenu.Share -> shareFile(result.node)
                is CellFileActionsMenu.CancelLoading -> cancelDownload(result.node.uuid)
            }
        }
    }

    internal fun editNode(nodeUuid: String) = viewModelScope.launch {
        getEditorUrl(nodeUuid)
            .onSuccess { url ->
                if (url != null) {
                    onlineEditor.open(url)
                } else {
                    sendAction(ShowEditErrorDialog(nodeUuid))
                }
            }
            .onFailure {
                sendAction(ShowEditErrorDialog(nodeUuid))
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
        _isDeleteInProgress.value = true
        val localPath = if (node is CellNodeUi.File) {
            node.localPath
        } else {
            null
        }

        removeFromListUi(node)

        deleteCellAsset(node.uuid, localPath)
            .onSuccess {
                _isDeleteInProgress.value = false
                sendAction(HideDeleteConfirmation)
                sendAction(
                    ShowFileDeletedMessage(
                        isFile = node is CellNodeUi.File,
                        permanently = isRecycleBin()
                    )
                )
                refreshNodes()
                // Wait until refresh completes
                pagingRefreshDone.first()
            }
            .onFailure {
                _isDeleteInProgress.value = false
                sendAction(ShowError(CellError.OTHER_ERROR))
                addToListUi(node)
            }
    }

    private fun restoreNodeFromRecycleBin(node: CellNodeUi) {
        viewModelScope.launch {
            _isRestoreInProgress.value = true
            removeFromListUi(node)
            restoreNodeFromRecycleBinUseCase(navArgs.parentFolderUuid ?: node.uuid)
                .onSuccess {
                    _isRestoreInProgress.value = false
                    if (navArgs.parentFolderUuid != null) {
                        sendAction(HideRestoreParentFolderDialog)
                        _navigateToRecycleBinRoot.value = true
                        // delay to allow navigation to complete before refreshing data
                        delay(RESTORE_DELAY_MS)
                    } else {
                        sendAction(HideRestoreConfirmation)
                    }
                    refreshNodes()
                }
                .onFailure {
                    _isRestoreInProgress.value = false
                    addToListUi(node)
                    if (navArgs.parentFolderUuid != null) {
                        sendAction(HideRestoreParentFolderDialog)
                    } else {
                        sendAction(HideRestoreConfirmation)
                    }
                    sendAction(ShowUnableToRestoreDialog(node is CellNodeUi.Folder))
                }
        }
    }

    private fun removeFromListUi(node: CellNodeUi) = removedItemsFlow.update { it + node.uuid }
    private fun addToListUi(node: CellNodeUi) = removedItemsFlow.update { it - node.uuid }
    fun clearRemovedItems() = removedItemsFlow.update { emptyList() }

    private fun updateDownloadData(uuid: String, block: () -> DownloadData) {
        val data = block()
        // Persist to the process-scoped cache so other CellViewModel instances (e.g. AllFiles ↔ Search)
        // can see locally-available files without re-downloading.
        data.localPath?.toString()?.let { sharedPathCache.put(uuid, it) }
        downloadDataFlow.update { map ->
            val progressMap = map.toMutableMap()
            progressMap[uuid] = data
            progressMap.toImmutableMap()
        }
    }

    private fun updateOpenLoadState(uuid: String, block: () -> OpenLoadState) {
        openLoadStateFlow.update { map ->
            map.toMutableMap().apply { put(uuid, block()) }.toImmutableMap()
        }
    }

    private fun clearOpenLoadState(uuid: String) {
        openLoadStateFlow.update { map ->
            map.toMutableMap().apply { remove(uuid) }.toImmutableMap()
        }
    }

    internal fun clearAllErrorStates() {
        openLoadStateFlow.update { map ->
            map.toMutableMap().apply {
                entries.removeAll { it.value is OpenLoadState.Error }
            }.toImmutableMap()
        }
    }

    private fun loadWireCellConfig() = viewModelScope.launch {
        val config = getWireCellsConfig()
        isCollaboraEnabled = config?.collabora != CollaboraEdition.NO
    }

    companion object {
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
    data class OnItemClick(val file: CellNodeUi) : CellViewIntent
    data class OnItemMenuClick(val cellNode: CellNodeUi) : CellViewIntent
    data class OnMenuItemActionSelected(val node: CellNodeUi, val action: NodeBottomSheetAction) : CellViewIntent
    data class OnNodeDeleteConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnNodeRestoreConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnParentFolderRestoreConfirmed(val node: CellNodeUi) : CellViewIntent
    data class OnCancelDownload(val uuid: String) : CellViewIntent
    data object OnScreenLeave : CellViewIntent
}

sealed interface CellViewAction
internal data class ShowDeleteConfirmation(val node: CellNodeUi, val isPermanentDelete: Boolean) : CellViewAction
internal data object HideDeleteConfirmation : CellViewAction
internal data class ShowRestoreConfirmation(val node: CellNodeUi) : CellViewAction
internal data object HideRestoreConfirmation : CellViewAction
internal data class ShowError(val error: CellError) : CellViewAction
internal data class ShowPublicLinkScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowRenameScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowAddRemoveTagsScreen(val cellNode: CellNodeUi) : CellViewAction
internal data class ShowMoveToFolderScreen(val currentPath: String, val nodeToMovePath: String, val uuid: String) : CellViewAction
internal data class ShowVersionHistoryScreen(val uuid: String, val fileName: String) : CellViewAction
internal data class ShowUnableToRestoreDialog(val isFolder: Boolean) : CellViewAction
internal data class ShowRestoreParentFolderDialog(val cellNode: CellNodeUi) : CellViewAction
internal data object HideRestoreParentFolderDialog : CellViewAction
internal data class ShowFileDeletedMessage(val isFile: Boolean, val permanently: Boolean) : CellViewAction
internal data object RefreshData : CellViewAction
internal data class OpenFolder(val path: String, val title: String, val parentFolderUuid: String?) : CellViewAction
internal data class ShowEditErrorDialog(val nodeUuid: String) : CellViewAction

internal enum class CellError(val message: Int) {
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

internal sealed interface OpenLoadState {
    data class Loading(val progress: Float = 0f) : OpenLoadState
    data class Ready(val localPath: Path) : OpenLoadState
    data object Error : OpenLoadState
}

private const val RESTORE_DELAY_MS = 300L
private const val OPEN_SPINNER_DELAY_MS = 300L
private const val OPEN_READY_DISMISS_MS = 3_000L
