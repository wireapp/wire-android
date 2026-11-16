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

import com.wire.android.feature.cells.ui.dialog.BulkRestoreDialog
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodesFromRecycleBinUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Restores nodes from the recycle bin, one at a time or the bulk selection, and tells the other screens about it.
 * One instance per screen: it holds the state of the restorations in progress.
 */
class CellRestoreController @Inject constructor(
    private val restoreNodeFromRecycleBin: RestoreNodeFromRecycleBinUseCase,
    private val restoreNodesFromRecycleBin: RestoreNodesFromRecycleBinUseCase,
    private val nodeChangeEvents: CellNodeChangeEvents,
) {

    private val _isRestoreInProgress = MutableStateFlow(false)
    val isRestoreInProgress: StateFlow<Boolean> = _isRestoreInProgress.asStateFlow()

    // Used to navigate to the root of the recycle bin after restoring a parent folder.
    private val _navigateToRecycleBinRoot = MutableStateFlow(false)
    val navigateToRecycleBinRoot: StateFlow<Boolean> = _navigateToRecycleBinRoot.asStateFlow()

    // Kept in the state, not sent as actions, so the dialog survives configuration changes.
    private val _bulkDialog = MutableStateFlow<BulkRestoreDialog?>(null)
    internal val bulkDialog: StateFlow<BulkRestoreDialog?> = _bulkDialog.asStateFlow()

    /**
     * Restores [node], taking it off the list right away and putting it back if that fails. Inside a deleted folder,
     * nothing can be restored on its own: it's the whole top deleted folder that is restored.
     */
    internal fun restore(node: CellNodeUi, host: CellScreenHost) = host.scope.launch {
        val parentFolderUuid = host.context.parentFolderUuid
        val hideDialogAction = if (parentFolderUuid != null) HideRestoreParentFolderDialog else HideRestoreConfirmation
        _isRestoreInProgress.value = true
        host.nodeList.hide(node)
        restoreNodeFromRecycleBin(parentFolderUuid ?: node.uuid)
            .onSuccess {
                _isRestoreInProgress.value = false
                notifyNodesRestored(host)
                host.sendAction(hideDialogAction)
                if (parentFolderUuid != null) {
                    _navigateToRecycleBinRoot.value = true
                    // delay to allow navigation to complete before refreshing data
                    delay(RESTORE_DELAY_MS.milliseconds)
                }
                host.nodeList.refresh()
            }
            .onFailure {
                _isRestoreInProgress.value = false
                host.nodeList.show(node)
                host.sendAction(hideDialogAction)
                host.sendAction(ShowUnableToRestoreDialog(node is CellNodeUi.Folder))
            }
    }

    internal fun showBulkConfirmation(nodes: List<CellNodeUi>, host: CellScreenHost) {
        if (nodes.isEmpty()) return
        if (host.context.parentFolderUuid != null) {
            // Inside a deleted folder, nothing can be restored on its own: like for a single node, it's the folder or nothing.
            host.sendAction(ShowRestoreParentFolderDialog(nodes.first()))
        } else {
            _bulkDialog.value = BulkRestoreDialog.Confirmation(nodes)
        }
    }

    /**
     * Restores the confirmed nodes in a single request. There is no rollback: if it fails midway, some of them may still
     * have been restored, so the list is refreshed and the selection cleared either way.
     */
    internal fun restoreConfirmed(host: CellScreenHost) {
        val confirmation = _bulkDialog.value as? BulkRestoreDialog.Confirmation ?: return
        if (confirmation.isInProgress) return
        val nodes = confirmation.nodes
        _bulkDialog.value = confirmation.copy(isInProgress = true)
        host.scope.launch {
            val result = restoreNodesFromRecycleBin(nodes.map { it.uuid })
            // Even on failure: without rollback, part of the nodes may have been restored.
            notifyNodesRestored(host)
            host.clearSelection()
            result
                .onSuccess {
                    _bulkDialog.value = null
                    nodes.forEach { host.nodeList.hide(it) }
                    host.sendAction(ShowNodesRestoredMessage(restoredCount = nodes.size))
                }
                .onFailure { failure ->
                    _bulkDialog.value = BulkRestoreDialog.Failure(
                        totalCount = nodes.size,
                        isNoNetworkConnection = failure is NetworkFailure.NoNetworkConnection,
                    )
                }
            host.nodeList.refresh()
        }
    }

    internal fun dismissBulkDialog() {
        // The confirmation stays up until the request completes.
        if ((_bulkDialog.value as? BulkRestoreDialog.Confirmation)?.isInProgress == true) return
        _bulkDialog.value = null
    }

    private fun notifyNodesRestored(host: CellScreenHost) {
        host.context.rootConversationId?.let { nodeChangeEvents.onNodesRestored(it) }
    }

    private companion object {
        const val RESTORE_DELAY_MS = 300L
    }
}
