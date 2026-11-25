/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.feature.cells.ui.dialog.BulkDeleteDialog
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.kalium.cells.domain.model.NodeToDelete
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetsUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deletes nodes, one at a time or the bulk selection: to the recycle bin, or for good from the recycle bin itself.
 * One instance per screen: it holds the state of the deletions in progress.
 */
class CellDeleteController @Inject constructor(
    private val deleteCellAsset: DeleteCellAssetUseCase,
    private val deleteCellAssets: DeleteCellAssetsUseCase,
) {

    private val _isDeleteInProgress = MutableStateFlow(false)
    val isDeleteInProgress: StateFlow<Boolean> = _isDeleteInProgress.asStateFlow()

    // Kept in the state, not sent as actions, so the dialog survives configuration changes.
    private val _bulkDialog = MutableStateFlow<BulkDeleteDialog?>(null)
    internal val bulkDialog: StateFlow<BulkDeleteDialog?> = _bulkDialog.asStateFlow()

    /** Deletes [node], taking it off the list right away and putting it back if that fails. */
    internal fun delete(node: CellNodeUi, host: CellScreenHost) = host.scope.launch {
        _isDeleteInProgress.value = true
        host.nodeList.hide(node)

        // For a folder, its path lets the local copies of the files inside it be cleaned up too.
        deleteCellAsset(
            node.uuid,
            (node as? CellNodeUi.File)?.localPath,
            folderPath = (node as? CellNodeUi.Folder)?.remotePath,
        )
            .onSuccess {
                _isDeleteInProgress.value = false
                host.sendAction(HideDeleteConfirmation)
                host.sendAction(ShowFileDeletedMessage(isFile = node is CellNodeUi.File, permanently = host.context.isRecycleBin))
                host.nodeList.refresh()
                host.nodeList.awaitRefreshDone()
            }
            .onFailure {
                _isDeleteInProgress.value = false
                host.sendAction(ShowError(CellError.OTHER_ERROR))
                host.nodeList.show(node)
            }
    }

    internal fun showBulkConfirmation(nodes: List<CellNodeUi>, host: CellScreenHost) {
        if (nodes.isEmpty()) return
        // In the recycle bin, deleting means deleting for good.
        _bulkDialog.value = BulkDeleteDialog.Confirmation(nodes, isPermanentDelete = host.context.isRecycleBin)
    }

    /**
     * Deletes the confirmed nodes in a single request. There is no rollback: if it fails midway, some of them may still
     * have been deleted, so the list is refreshed and the selection cleared either way.
     */
    internal fun deleteConfirmed(host: CellScreenHost) {
        val confirmation = _bulkDialog.value as? BulkDeleteDialog.Confirmation ?: return
        if (confirmation.isInProgress) return
        val nodes = confirmation.nodes
        val isPermanentDelete = confirmation.isPermanentDelete
        _bulkDialog.value = confirmation.copy(isInProgress = true)
        host.scope.launch {
            val result = deleteCellAssets(nodes.map { it.toNodeToDelete() }, permanentDelete = isPermanentDelete)
            host.clearSelection()
            result
                .onSuccess {
                    _bulkDialog.value = null
                    nodes.forEach { host.nodeList.hide(it) }
                    host.sendAction(
                        nodes.singleOrNull()
                            ?.let { ShowFileDeletedMessage(isFile = it is CellNodeUi.File, permanently = isPermanentDelete) }
                            ?: ShowNodesDeletedMessage(deletedCount = nodes.size, permanently = isPermanentDelete)
                    )
                }
                .onFailure { failure ->
                    _bulkDialog.value = BulkDeleteDialog.Failure(
                        totalCount = nodes.size,
                        isNoNetworkConnection = failure is NetworkFailure.NoNetworkConnection,
                        isPermanentDelete = isPermanentDelete,
                    )
                }
            host.nodeList.refresh()
        }
    }

    internal fun dismissBulkDialog() {
        // The confirmation stays up until the request completes.
        if ((_bulkDialog.value as? BulkDeleteDialog.Confirmation)?.isInProgress == true) return
        _bulkDialog.value = null
    }

    // Deleted by uuid, so it doesn't matter where the node lives, e.g. in the recycle bin rather than the folder browsed.
    private fun CellNodeUi.toNodeToDelete() = NodeToDelete(
        uuid = uuid,
        localPath = (this as? CellNodeUi.File)?.localPath,
        folderPath = (this as? CellNodeUi.Folder)?.remotePath,
    )
}
