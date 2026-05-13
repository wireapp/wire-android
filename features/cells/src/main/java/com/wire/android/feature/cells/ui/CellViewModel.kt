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
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toKaliumCriteria
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.data.FileFilters
import com.wire.kalium.cells.data.SortingSpec
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetConversationNameUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetUserNameUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.offline.DeleteOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.GetOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.common.functional.fold
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.featureConfig.CollaboraEdition
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class CellViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase,
    private val isCellAvailable: IsAtLeastOneCellAvailableUseCase,
    private val fileHelper: FileHelper,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val cellFileActionsMenu: CellFileActionsMenu,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    private val openFileDownloadController: OpenFileDownloadController,
    private val offlineFileDownloadController: OfflineFileDownloadController,
    private val observeOfflineFiles: ObserveOfflineFilesUseCase,
    private val deleteOfflineFile: DeleteOfflineFileUseCase,
    private val getOfflineFile: GetOfflineFileUseCase,
    private val networkStateObserver: NetworkStateObserver,
    private val getConversationName: GetConversationNameUseCase,
    private val getUserName: GetUserNameUseCase,
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

    internal val fileReadyFlow: Flow<CellNodeUi.File> = sharedPathCache.fileReadyEvents

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

    // AllFiles context (no conversationId, not recycle bin) defaults to newest-first;
    // ConversationFiles and RecycleBin default to folders-first.
    private val _defaultSortingCriteria = MutableStateFlow(
        if (navArgs.conversationId == null && !(navArgs.isRecycleBin ?: false)) {
            SortingCriteria.ByDate.NewestFirst
        } else {
            SortingCriteria.FoldersFirst
        }
    )

    val isOnline: StateFlow<Boolean> = networkStateObserver.observeNetworkState()
        .map { it is NetworkState.ConnectedWithInternet }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = networkStateObserver.observeNetworkState().value is NetworkState.ConnectedWithInternet,
        )

    private var isCollaboraEnabled: Boolean = false

    init {
        loadWireCellConfig()
        checkCellAvailabilityAndRefresh()
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
            _defaultSortingCriteria.flatMapLatest { sortingCriteria ->
                combine(
                    getCellFilesPaged(
                        conversationId = navArgs.conversationId,
                        fileFilters = FileFilters(
                            onlyDeleted = navArgs.isRecycleBin ?: false,
                        ),
                        sortingSpec = SortingSpec(
                            criteria = sortingCriteria.toKaliumCriteria(),
                            descending = sortingCriteria.isDescending,
                        ),
                    ).cachedIn(viewModelScope),
                    removedItemsFlow,
                    sharedPathCache.openLoadStates,
                    observeOfflineFiles(),
                    offlineFileDownloadController.downloadProgresses,
                ) { pagingData, removedItems, openLoadStates, offlineFiles, downloadProgresses ->
                    val offlineUuids = offlineFiles.map { it.id }.toSet()
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
                                is Node.Folder -> node.toUiModel()
                                is Node.File -> node.toUiModel(
                                    openLoadState = openLoadState,
                                    downloadProgress = downloadProgresses[node.uuid],
                                    isAvailableOffline = node.uuid in offlineUuids,
                                )
                            }
                        }
                }
            }
        }
    }.shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 1)

    private val offlineNodesFlow: Flow<PagingData<CellNodeUi>> =
        combine(
            observeOfflineFiles(),
            sharedPathCache.openLoadStates,
            offlineFileDownloadController.downloadProgresses,
        ) { offlineFiles, openLoadStates, downloadProgresses ->
            val rootConversationId = navArgs.conversationId?.substringBefore("/")
            val filtered = if (rootConversationId != null) {
                offlineFiles.filter { it.conversationId == rootConversationId }
            } else {
                offlineFiles
            }
            PagingData.from(
                data = filtered.map { info ->
                    info.toCellNodeUi(
                        conversationName = info.conversationId?.let { getConversationName(it) },
                        userName = info.owner.ifEmpty { null }?.let { getUserName(it) },
                        openLoadState = openLoadStates[info.id],
                        downloadProgress = downloadProgresses[info.id],
                    )
                },
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(true),
                    prepend = LoadState.NotLoading(true),
                    append = LoadState.NotLoading(true),
                )
            )
        }

    internal val nodesFlow = combine(cellAvailableFlow, isOnline) { cellAvailable, online ->
        cellAvailable to online
    }.flatMapLatest { (cellAvailable, online) ->
        when {
            !cellAvailable || searchNavArgs != null -> flowOf(emptyData)
            !online -> offlineNodesFlow
            else -> sharedNodesFlow
        }
    }

    fun onPullToRefresh() {
        if (!isOnline.value) return
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
            cellNode.openLoadState is OpenLoadState.Ready -> openLocalFile(cellNode)
            cellNode.openLoadState is OpenLoadState.Loading -> cancelOpenDownload(cellNode.uuid)
            cellNode.downloadProgress != null -> offlineFileDownloadController.cancel(cellNode.uuid, viewModelScope)
            cellNode.localFileAvailable() -> openLocalFile(cellNode)
            cellNode.openLoadState is OpenLoadState.Error -> startOpenDownload(cellNode)
            cellNode.canOpenWithUrl() -> openFileContentUrl(cellNode)
            else -> startOpenDownload(cellNode)
        }
    }

    private fun startOpenDownload(cellNode: CellNodeUi.File) {
        openFileDownloadController.start(
            scope = viewModelScope,
            cellNode = cellNode,
            onOpenFile = ::openLocalFile,
            onError = { sendAction(ShowError(it)) },
        )
    }

    internal fun cancelOpenDownload(uuid: String) {
        openFileDownloadController.cancel(uuid, viewModelScope)
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
            isOnline = isOnline.value,
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
                is CellFileActionsMenu.Open -> sendIntent(CellViewIntent.OnItemClick(result.node))
                is CellFileActionsMenu.Edit -> editNode(result.node.uuid)
                is CellFileActionsMenu.Share -> shareFile(result.node)
                is CellFileActionsMenu.CancelLoading -> cancelDownload(result.node.uuid)
                is CellFileActionsMenu.CancelDownload -> offlineFileDownloadController.cancel(result.node.uuid, viewModelScope)
                is CellFileActionsMenu.MakeAvailableOffline -> makeAvailableOffline(result.node)
                is CellFileActionsMenu.RemoveOfflineAccess -> removeOfflineAccess(result.node)
            }
        }
    }

    private fun makeAvailableOffline(node: CellNodeUi.File) {
        offlineFileDownloadController.start(
            scope = viewModelScope,
            cellNode = node.copy(
                conversationId = navArgs.conversationId
            ),
            onSuccess = { _ -> sendAction(ShowOfflineFileSaved) },
            onError = { sendAction(ShowError(it)) },
        )
    }

    private fun removeOfflineAccess(node: CellNodeUi.File) = viewModelScope.launch {
        val localPath = getOfflineFile(node.uuid)?.localPath
            ?: node.localPath
            ?: sharedPathCache.getCompletedPath(node.uuid)

        // Remove the DB record so the UI stops showing the offline indicator.
        deleteOfflineFile(node.uuid)

        // Delete the physical file from device storage
        localPath?.takeIf { it.isNotBlank() }?.let { path ->
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                File(path).delete()
            }
        }

        sharedPathCache.clearCompletedPath(node.uuid)
        sharedPathCache.clearOpenLoadState(node.uuid)
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

    private fun loadWireCellConfig() = viewModelScope.launch {
        val config = getWireCellsConfig()
        isCollaboraEnabled = config?.collabora != CollaboraEdition.NO
    }

    private fun OfflineFileInfo.toCellNodeUi(
        conversationName: String? = null,
        userName: String? = null,
        openLoadState: OpenLoadState? = null,
        downloadProgress: Float? = null,
    ): CellNodeUi.File {
        val extension = name.substringAfterLast('.', "")
        return CellNodeUi.File(
            uuid = id,
            conversationId = conversationId,
            name = name,
            mimeType = "",
            assetType = AttachmentFileType.fromExtension(extension),
            size = size,
            localPath = localPath,
            ownerUserId = owner.ifEmpty { null },
            userName = userName,
            userHandle = null,
            conversationName = conversationName,
            modifiedTime = modifiedAt,
            isAvailableOffline = true,
            openLoadState = openLoadState,
            downloadProgress = downloadProgress,
        )
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
internal data object ShowOfflineFileSaved : CellViewAction

internal enum class CellError(val message: Int) {
    NO_APP_FOUND(R.string.no_app_found),
    OTHER_ERROR(R.string.action_failed),
    DOWNLOAD_FAILED(R.string.action_failed),
    NO_SPACE_LEFT(R.string.no_space_left_error),
}

data class MenuOptions(
    val node: CellNodeUi,
    val actions: List<NodeBottomSheetAction>
)

private const val RESTORE_DELAY_MS = 300L
