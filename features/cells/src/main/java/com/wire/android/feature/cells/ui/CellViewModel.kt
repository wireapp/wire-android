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

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.wire.android.feature.cells.ui.dialog.BulkDeleteDialog
import com.wire.android.feature.cells.ui.dialog.BulkOfflineDialog
import com.wire.android.feature.cells.ui.dialog.BulkRestoreDialog
import com.wire.android.feature.cells.ui.dialog.EmptyRecycleBinDialog
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.ui.model.canOpenWithUrl
import com.wire.android.feature.cells.ui.model.isOfflineDownloadPending
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the files screens: All Files, the files of a conversation (shared drive) and its recycle bin, and the search
 * screen. Routes the user's intents to the collaborators doing the actual work, and exposes their state to the screen.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class CellViewModel @AssistedInject constructor(
    @Assisted navArgs: CellFilesNavArgs,
    @Assisted searchNavArgs: SearchNavArgs?,
    private val featureFlags: CellFeatureFlags,
    networkStateObserver: NetworkStateObserver,
    nodesSource: CellNodesSource,
    private val fileOpener: CellFileOpener,
    private val availabilityChecker: CellNodeAvailabilityChecker,
    private val deleteController: CellDeleteController,
    private val restoreController: CellRestoreController,
    private val emptyRecycleBinController: EmptyRecycleBinController,
    private val offlineAccess: CellOfflineAccessController,
    private val offlineAccessRules: OfflineAccessRules,
    private val uploadController: CellUploadController,
    private val viewerAccess: CellViewerAccess,
    private val hints: CellScreenHints,
    private val cellFileActionsMenu: CellFileActionsMenu,
    nodeChangesObserver: CellNodeChangesObserver,
    private val nodeListRegistry: CellNodeListRegistry,
    sharedPathCache: CellFileLocalPathCache,
) : ActionsViewModel<CellViewAction>() {

    @AssistedFactory
    interface Factory {
        fun create(navArgs: CellFilesNavArgs, searchNavArgs: SearchNavArgs?): CellViewModel
    }

    private val screenContext = CellScreenContext(navArgs, searchNavArgs)

    val offlineFilesEnabled: Boolean get() = featureFlags.offlineFilesEnabled
    val drivePermissionsEnabled: Boolean get() = featureFlags.drivePermissionsEnabled
    val driveDirectUploadEnabled: Boolean get() = featureFlags.driveDirectUploadEnabled
    internal val bulkRestoreEnabled: Boolean get() = featureFlags.bulkRestoreEnabled

    /** Whether the bulk actions bar offers the offline actions (make available / remove offline access). */
    val bulkOfflineActionsEnabled: Boolean get() = featureFlags.bulkOfflineActionsEnabled

    val isOnline: StateFlow<Boolean> = networkStateObserver.observeNetworkState()
        .map { it is NetworkState.ConnectedWithInternet }
        .stateInScope(networkStateObserver.observeNetworkState().value is NetworkState.ConnectedWithInternet)

    private val _isSelfUserViewerOnly = MutableStateFlow(false)
    internal val isSelfUserViewerOnly: StateFlow<Boolean> = _isSelfUserViewerOnly.asStateFlow()

    // Show menu with actions for the selected file.
    private val _menu: MutableSharedFlow<MenuOptions> = MutableSharedFlow()
    internal val menu: SharedFlow<MenuOptions> = _menu.asSharedFlow()

    internal val fileReadyFlow: Flow<CellNodeUi.File> = sharedPathCache.fileReadyEvents

    private val bulkSelection = CellBulkSelection(
        isSupported = featureFlags.driveBulkActionsEnabled &&
                screenContext.offersBulkSelection(featureFlags.bulkOfflineActionsEnabled),
        isOnline = isOnline,
        isSelfUserViewerOnly = isSelfUserViewerOnly,
        scope = viewModelScope,
    )
    internal val selectedNodes: StateFlow<Map<String, CellNodeUi>> = bulkSelection.selectedNodes
    internal val isBulkSelectionEnabled: StateFlow<Boolean> = bulkSelection.isEnabled

    // The search screen starts from the unfiltered list of the listing it was opened from, which stays open below it:
    // share that list rather than load it again. Without it, e.g. opened from a conversation, load one of its own.
    private val sharedNodeList: CellNodeList? =
        if (screenContext.isSearch) nodeListRegistry.find(screenContext.currentPath) else null

    private val nodeList: CellNodeList = sharedNodeList ?: CellNodeList(
        scope = viewModelScope,
        context = screenContext,
        source = nodesSource,
        isOnline = isOnline,
        offlineFilesEnabled = featureFlags.offlineFilesEnabled,
        // Not in the recycle bin, nor in search unless opened from a listing showing only offline files.
        offlineOnlyFilterEnabled = featureFlags.offlineFilesEnabled &&
                (screenContext.isAllFiles || screenContext.isConversationFiles) &&
                (!screenContext.isSearch || screenContext.isOfflineOnlyRequested),
    )

    private val host = object : CellScreenHost {
        override val scope: CoroutineScope get() = viewModelScope
        override val context: CellScreenContext get() = screenContext
        override val nodeList: CellNodeListUpdater get() = this@CellViewModel.nodeList
        override fun clearSelection() = bulkSelection.clear()
        override fun sendAction(action: CellViewAction) = this@CellViewModel.sendAction(action)
    }

    internal val nodesFlow: Flow<PagingData<CellNodeUi>> = nodeList.nodes
    val isPullToRefresh: StateFlow<Boolean> = nodeList.isPullToRefresh
    val sortingCriteria: StateFlow<SortingCriteria> = nodeList.sortingCriteria
    val defaultSortingCriteria: SortingCriteria = screenContext.defaultSortingCriteria
    internal val offlineOnlyFilterEnabled: Boolean = nodeList.offlineOnlyFilterEnabled
    internal val isOfflineOnly: StateFlow<Boolean> = nodeList.isOfflineOnly
    internal val showOfflineOnlyToggle: StateFlow<Boolean> = nodeList.showOfflineOnlyToggle

    val isDeleteInProgress: StateFlow<Boolean> = deleteController.isDeleteInProgress
    val isRestoreInProgress: StateFlow<Boolean> = restoreController.isRestoreInProgress
    val navigateToRecycleBinRoot: StateFlow<Boolean> = restoreController.navigateToRecycleBinRoot

    // Dialogs are kept in the state, not sent as actions, so that they survive configuration changes.
    internal val bulkDeleteDialog: StateFlow<BulkDeleteDialog?> = deleteController.bulkDialog
    internal val bulkRestoreDialog: StateFlow<BulkRestoreDialog?> = restoreController.bulkDialog
    internal val emptyRecycleBinDialog: StateFlow<EmptyRecycleBinDialog?> = emptyRecycleBinController.dialog
    internal val bulkOfflineDialog: StateFlow<BulkOfflineDialog?> = offlineAccess.bulkDialog
    internal val uploadConfirmation: StateFlow<UploadConfirmation?> = uploadController.confirmation

    internal val canMakeSelectionAvailableOffline: StateFlow<Boolean> = selectedNodes
        .map { offlineAccessRules.canMakeAnyAvailableOffline(it.values) }
        .stateInScope(false)

    internal val canRemoveSelectionOfflineAccess: StateFlow<Boolean> = selectedNodes
        .map { offlineAccessRules.canRemoveAnyOfflineAccess(it.values) }
        .stateInScope(false)

    /** Emptying the recycle bin is offered at its root only, behind the bulk actions flag, while online and for editors. */
    internal val canEmptyRecycleBin: StateFlow<Boolean> = combine(isOnline, isSelfUserViewerOnly) { online, viewerOnly ->
        featureFlags.driveBulkActionsEnabled && screenContext.isRecycleBinRoot && online && !viewerOnly
    }.stateInScope(false)

    internal val showViewerAccessBanner: StateFlow<Boolean> =
        hints.viewerAccessBanner(screenContext.rootConversationId, isSelfUserViewerOnly).stateInScope(false)

    internal val showBulkSelectionHint: StateFlow<Boolean> =
        hints.bulkSelectionHint(bulkSelection.isEnabled).stateInScope(false)

    internal val showOfflineToggleHint: StateFlow<Boolean> =
        hints.offlineToggleHint(showOfflineOnlyToggle, showBulkSelectionHint).stateInScope(false)

    init {
        offlineAccess.observeDownloads(host)
        viewModelScope.launch { fileOpener.loadConfig() }
        viewModelScope.launch { _isSelfUserViewerOnly.value = viewerAccess.isViewerOnly(screenContext) }
        // A shared list is already loaded, and kept in sync, by the listing that owns it.
        if (sharedNodeList == null) {
            nodeList.load()
            nodeChangesObserver.observe(host)
        }
        if (screenContext.isSearchableListing) nodeListRegistry.register(screenContext.currentPath, nodeList)
        dismissBulkSelectionHintOnceSelectionIsUsed()
    }

    override fun onCleared() {
        if (screenContext.isSearchableListing) nodeListRegistry.unregister(screenContext.currentPath, nodeList)
        super.onCleared()
    }

    internal fun currentNodeUuid(): String? = screenContext.currentPath
    internal fun isRecycleBin(): Boolean = screenContext.isRecycleBin
    internal fun screenTitle(): String? = screenContext.screenTitle
    internal fun breadcrumbs(): Array<String>? = screenContext.breadcrumbs

    fun onPullToRefresh() = nodeList.onPullToRefresh()
    fun onNodeCreated() = nodeList.refresh()
    fun clearRemovedItems() = nodeList.clearHidden()
    fun setSortBy(by: SortBy) = nodeList.setSortBy(by)
    fun setSorting(criteria: SortingCriteria) = nodeList.setSorting(criteria)

    internal fun setOfflineOnly(offlineOnly: Boolean) {
        val isToggleHintShown = showOfflineToggleHint.value
        if (!nodeList.setOfflineOnly(offlineOnly)) return
        // Once the toggle is found, the tip has nothing left to tell.
        if (isToggleHintShown) onOfflineToggleHintDismissed()
        // The selection was made in the other listing.
        bulkSelection.clear()
    }

    internal fun onViewerAccessBannerDismissed() {
        val conversationId = screenContext.rootConversationId ?: return
        viewModelScope.launch { hints.dismissViewerAccessBanner(conversationId) }
    }

    internal fun onBulkSelectionHintDismissed() {
        viewModelScope.launch { hints.dismissBulkSelectionHint() }
    }

    internal fun onOfflineToggleHintDismissed() {
        viewModelScope.launch { hints.dismissOfflineToggleHint() }
    }

    internal fun confirmUpload() = uploadController.confirm(host)
    internal fun cancelUploadConfirmation() = uploadController.cancel()

    internal fun editNode(nodeUuid: String) = fileOpener.edit(nodeUuid, viewModelScope, host)

    @Suppress("CyclomaticComplexMethod")
    internal fun sendIntent(intent: CellViewIntent) {
        val selected = bulkSelection.selectedNodes.value.values
        when (intent) {
            is CellViewIntent.OnItemClick -> onItemClick(intent.file)
            is CellViewIntent.OnItemLongClick -> bulkSelection.onLongClick(intent.cellNode)
            is CellViewIntent.OnDragSelection -> bulkSelection.onDragSelection(intent.nodes)
            is CellViewIntent.OnDragSelectionEnd -> bulkSelection.onDragSelectionEnd()
            is CellViewIntent.OnClearSelection -> bulkSelection.clear()
            is CellViewIntent.OnMoveSelectedNodesClick -> moveSelectedNodes()

            is CellViewIntent.OnDeleteSelectedNodesClick -> deleteController.showBulkConfirmation(selected.toList(), host)
            is CellViewIntent.OnBulkDeleteConfirmed -> deleteController.deleteConfirmed(host)
            is CellViewIntent.OnBulkDeleteDialogDismissed -> deleteController.dismissBulkDialog()
            is CellViewIntent.OnNodeDeleteConfirmed -> deleteController.delete(intent.node, host)

            is CellViewIntent.OnRestoreSelectedNodesClick -> restoreController.showBulkConfirmation(selected.toList(), host)
            is CellViewIntent.OnBulkRestoreConfirmed -> restoreController.restoreConfirmed(host)
            is CellViewIntent.OnBulkRestoreDialogDismissed -> restoreController.dismissBulkDialog()
            is CellViewIntent.OnNodeRestoreConfirmed -> restoreController.restore(intent.node, host)
            is CellViewIntent.OnParentFolderRestoreConfirmed -> restoreController.restore(intent.node, host)

            is CellViewIntent.OnEmptyRecycleBinClick -> if (canEmptyRecycleBin.value) emptyRecycleBinController.showConfirmation()
            is CellViewIntent.OnEmptyRecycleBinConfirmed -> emptyRecycleBinController.emptyConfirmed(host)
            is CellViewIntent.OnEmptyRecycleBinDialogDismissed -> emptyRecycleBinController.dismissDialog()

            is CellViewIntent.OnMakeSelectedNodesAvailableOfflineClick -> offlineAccess.showBulkConfirmation(selected)
            is CellViewIntent.OnRemoveSelectedNodesOfflineAccessClick -> offlineAccess.showBulkRemoveConfirmation(selected)
            is CellViewIntent.OnBulkOfflineConfirmed -> offlineAccess.onBulkConfirmed(host)
            is CellViewIntent.OnBulkOfflineDialogDismissed -> offlineAccess.dismissBulkDialog()

            is CellViewIntent.OnItemMenuClick -> onItemMenuClick(intent.cellNode)
            is CellViewIntent.OnMenuItemActionSelected -> onMenuItemAction(intent.node, intent.action)
            is CellViewIntent.OnCancelDownload -> cancelOpenDownload(intent.uuid)
            is CellViewIntent.OnFilesPickedForUpload -> uploadController.onFilesPicked(intent.uris, host)
        }
    }

    private fun onItemClick(node: CellNodeUi) {
        when {
            bulkSelection.isActive -> bulkSelection.toggle(node)
            node is CellNodeUi.File -> onFileClick(node)
            node is CellNodeUi.Folder -> onFolderClick(node)
        }
    }

    private fun onFileClick(file: CellNodeUi.File) {
        when {
            file.openLoadState is OpenLoadState.Loading -> cancelOpenDownload(file.uuid)
            file.isOfflineDownloadPending() -> offlineAccess.cancelDownload(file.uuid)
            fileOpener.isEditableDocument(file, isOnline.value) -> fileOpener.openEditableDocument(file, viewModelScope, host)
            file.openLoadState is OpenLoadState.Ready -> openLocalFile(file)
            file.localFileAvailable() -> openLocalFile(file)
            file.openLoadState is OpenLoadState.Error -> ifAvailable(file, ::startOpenDownload)
            file.canOpenWithUrl() -> ifAvailable(file) { fileOpener.openContentUrl(it, isSelfUserViewerOnly.value, host) }
            else -> ifAvailable(file, ::startOpenDownload)
        }
    }

    private fun onFolderClick(folder: CellNodeUi.Folder) {
        val path = screenContext.pathOf(folder) ?: run {
            sendAction(ShowError(CellError.OTHER_ERROR))
            return
        }
        sendAction(OpenFolder(path = path, title = folder.name ?: "", parentFolderUuid = screenContext.parentFolderUuidOf(folder)))
    }

    // Read when the file is actually opened, e.g. once downloaded, rather than when it was tapped.
    private fun openLocalFile(file: CellNodeUi.File) = fileOpener.openLocalFile(file, isSelfUserViewerOnly.value, host)

    private fun startOpenDownload(file: CellNodeUi.File) =
        fileOpener.startDownload(screenContext.withConversation(file), viewModelScope, host, ::openLocalFile)

    private fun cancelOpenDownload(uuid: String) {
        availabilityChecker.cancel(uuid)
        fileOpener.cancelDownload(uuid, viewModelScope)
    }

    private fun <T : CellNodeUi> ifAvailable(node: T, action: (T) -> Unit) =
        availabilityChecker.ifAvailable(node, isOnline.value, host, action)

    private fun moveSelectedNodes() {
        val folderPath = screenContext.currentPath ?: return
        val nodes = bulkSelection.toNodesToMove(folderPath)
        // Selection mode ends as the destination picker opens; the moved nodes leave this folder anyway.
        bulkSelection.clear()
        sendAction(ShowBulkMoveToFolderScreen(currentPath = folderPath.substringBefore("/"), nodes = nodes))
    }

    private fun onItemMenuClick(node: CellNodeUi) = viewModelScope.launch {
        val menuItems = cellFileActionsMenu.buildMenu(
            cellNode = node,
            isRecycleBin = screenContext.isRecycleBin,
            isConversationFiles = screenContext.isConversationFiles,
            isAllFiles = screenContext.isAllFiles,
            isSearching = screenContext.isSearchingDrive,
            isCollaboraEnabled = fileOpener.isCollaboraEnabled,
            // The folders of the offline listing only stand for the path of the files saved in them, there is no such
            // node to act on: they get the menu they'd get offline.
            isOnline = isOnline.value && !(isOfflineOnly.value && node is CellNodeUi.Folder),
            isOfflineOnly = isOfflineOnly.value,
        )
        _menu.emit(MenuOptions(node, menuItems))
    }

    private fun onMenuItemAction(node: CellNodeUi, action: NodeBottomSheetAction) {
        if (action.needsAvailableNode()) {
            ifAvailable(node) { performMenuItemAction(it, action) }
        } else {
            performMenuItemAction(node, action)
        }
    }

    /**
     * Whether the node must still be on the server for [this] to make sense. Opening is checked when it's routed to
     * [onFileClick]; the others only deal with local data or local work.
     */
    private fun NodeBottomSheetAction.needsAvailableNode() = this !in setOf(
        NodeBottomSheetAction.OPEN,
        NodeBottomSheetAction.CANCEL_LOADING,
        NodeBottomSheetAction.CANCEL_DOWNLOAD,
        NodeBottomSheetAction.REMOVE_OFFLINE_ACCESS,
    )

    private fun performMenuItemAction(node: CellNodeUi, action: NodeBottomSheetAction) {
        cellFileActionsMenu.onMenuItemAction(
            conversationId = screenContext.currentPath,
            parentFolderUuid = screenContext.parentFolderUuid,
            node = node,
            action = action,
        ) { result ->
            when (result) {
                is CellFileActionsMenu.Action -> sendAction(result.action)
                is CellFileActionsMenu.Open -> sendIntent(CellViewIntent.OnItemClick(result.node))
                is CellFileActionsMenu.Edit -> editNode(result.node.uuid)
                is CellFileActionsMenu.Share -> fileOpener.share(result.node, host)
                is CellFileActionsMenu.CancelLoading -> cancelOpenDownload(result.node.uuid)
                is CellFileActionsMenu.CancelDownload -> offlineAccess.cancelDownload(result.node.uuid)
                is CellFileActionsMenu.MakeAvailableOffline -> offlineAccess.makeAvailableOffline(result.node, host)
                is CellFileActionsMenu.RemoveOfflineAccess -> offlineAccess.removeOfflineAccess(result.node, host)
            }
        }
    }

    // Entering selection mode means the gesture was found: the tip isn't needed anymore.
    private fun dismissBulkSelectionHintOnceSelectionIsUsed() = viewModelScope.launch {
        // Only while the tip is shown: no need to store anything for users who already dismissed it.
        showBulkSelectionHint.first { it }
        bulkSelection.selectedNodes.first { it.isNotEmpty() }
        hints.dismissBulkSelectionHint()
    }

    private fun <T> Flow<T>.stateInScope(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initialValue)
}

internal fun SearchNavArgs.toCellFilesNavArgs(): CellFilesNavArgs =
    CellFilesNavArgs(conversationId = conversationId, isOfflineOnly = isOfflineOnly)
