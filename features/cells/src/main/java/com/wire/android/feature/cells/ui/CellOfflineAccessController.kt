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

import com.wire.android.feature.cells.ui.dialog.BulkOfflineDialog
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.isOfflineDownloadPending
import com.wire.android.feature.cells.ui.model.localFileAvailable
import com.wire.android.feature.cells.ui.offline.OfflineDownloadEvent
import com.wire.android.feature.cells.ui.offline.SessionOfflineDownloads
import com.wire.kalium.cells.domain.usecase.offline.DeleteOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.GetOfflineFileUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Saves files for offline use and removes their offline copy, one at a time or the bulk selection.
 *
 * Downloads run in the background for the whole app, see [SessionOfflineDownloads]: only the screen that requested
 * them reports how they went, and only while it's around. One instance per screen.
 */
class CellOfflineAccessController @Inject constructor(
    private val offlineDownloads: SessionOfflineDownloads,
    private val getOfflineFile: GetOfflineFileUseCase,
    private val deleteOfflineFile: DeleteOfflineFileUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    private val featureFlags: CellFeatureFlags,
) {

    // Kept in the state, not sent as actions, so the dialogs survive configuration changes.
    private val _bulkDialog = MutableStateFlow<BulkOfflineDialog?>(null)
    internal val bulkDialog: StateFlow<BulkOfflineDialog?> = _bulkDialog.asStateFlow()

    /** The offline download requests made from this screen, by id, and whether each was a bulk one. */
    private val requests = mutableMapOf<Long, Boolean>()

    /** Reports the outcome of the downloads requested from the screen of [host], for as long as it is around. */
    internal fun observeDownloads(host: CellScreenHost) = host.scope.launch {
        offlineDownloads.events.collect { event -> onDownloadEvent(event, host) }
    }

    /** Same rule as the single-item menu, see [CellFileActionsMenu]. */
    internal fun canBeMadeAvailableOffline(file: CellNodeUi.File): Boolean =
        !file.isAvailableOffline && !file.isOfflineDownloadPending() && featureFlags.collaboraIntegration && !file.isEditSupported

    /** At least one of [nodes] can be saved for offline use, see [canBeMadeAvailableOffline]. */
    internal fun canMakeAnyAvailableOffline(nodes: Collection<CellNodeUi>): Boolean =
        featureFlags.bulkOfflineActionsEnabled && nodes.any { it is CellNodeUi.File && canBeMadeAvailableOffline(it) }

    /** At least one of [nodes] is already saved for offline use. */
    internal fun canRemoveAnyOfflineAccess(nodes: Collection<CellNodeUi>): Boolean =
        featureFlags.bulkOfflineActionsEnabled && nodes.any { it is CellNodeUi.File && it.isAvailableOffline }

    internal fun makeAvailableOffline(file: CellNodeUi.File, host: CellScreenHost) =
        startDownloads(listOf(file), isBulk = false, host)

    /** Removes the offline copy of [file]. */
    internal fun removeOfflineAccess(file: CellNodeUi.File, host: CellScreenHost) = host.scope.launch { deleteOfflineCopy(file) }

    /** Cancels an offline download, whether it is running or still waiting for its turn. */
    fun cancelDownload(uuid: String) {
        offlineDownloads.cancel(uuid)
    }

    /**
     * Asks before saving the [selected] files for offline use. Folders, files already saved or downloading, and files
     * the single-item menu wouldn't offer it for are skipped, and the dialog says how many.
     * When the files clearly won't fit on the device, says so instead of offering to download them.
     */
    internal fun showBulkConfirmation(selected: Collection<CellNodeUi>) {
        val files = selected.filterIsInstance<CellNodeUi.File>().filter { canBeMadeAvailableOffline(it) }
        if (files.isEmpty()) return

        // Files already on the device are saved without being downloaded again.
        val requiredBytes = files.filterNot { it.localFileAvailable() }.sumOf { it.size ?: 0L }
        val availableBytes = offlineDownloads.availableStorageBytes()
        _bulkDialog.value = if (requiredBytes + STORAGE_SAFETY_MARGIN_BYTES > availableBytes) {
            BulkOfflineDialog.NotEnoughStorage(requiredBytes = requiredBytes, availableBytes = availableBytes)
        } else {
            BulkOfflineDialog.Confirmation(files = files, skippedCount = selected.size - files.size)
        }
    }

    /** Asks before removing the offline copy of the [selected] files; items not saved for offline use are skipped. */
    internal fun showBulkRemoveConfirmation(selected: Collection<CellNodeUi>) {
        val files = selected.filterIsInstance<CellNodeUi.File>().filter { it.isAvailableOffline }
        if (files.isEmpty()) return
        _bulkDialog.value = BulkOfflineDialog.RemoveConfirmation(files = files, skippedCount = selected.size - files.size)
    }

    /** Goes on with whatever the bulk dialog shown asked to confirm, or just closes it. */
    internal fun onBulkConfirmed(host: CellScreenHost) {
        when (val dialog = _bulkDialog.value) {
            is BulkOfflineDialog.Confirmation -> makeAvailableOffline(dialog.files, host)
            is BulkOfflineDialog.RemoveConfirmation -> removeOfflineAccess(dialog.files, host)
            else -> _bulkDialog.value = null
        }
    }

    internal fun dismissBulkDialog() {
        _bulkDialog.value = null
    }

    /** The files download in the background, and one message sums it all up once every download settled. */
    private fun makeAvailableOffline(files: List<CellNodeUi.File>, host: CellScreenHost) {
        _bulkDialog.value = null
        host.clearSelection()
        startDownloads(files, isBulk = true, host)
    }

    private fun removeOfflineAccess(files: List<CellNodeUi.File>, host: CellScreenHost) {
        _bulkDialog.value = null
        host.clearSelection()
        host.scope.launch {
            // Local work only, and quick: finish it even if the user leaves the screen meanwhile.
            withContext(NonCancellable) {
                files.forEach { deleteOfflineCopy(it) }
            }
            host.sendAction(ShowOfflineAccessRemoved(removedCount = files.size))
        }
    }

    private fun startDownloads(files: List<CellNodeUi.File>, isBulk: Boolean, host: CellScreenHost) {
        val requestId = offlineDownloads.download(files.map(host.context::withConversation)) ?: return
        requests[requestId] = isBulk
    }

    private fun onDownloadEvent(event: OfflineDownloadEvent, host: CellScreenHost) {
        when (event) {
            // Lets a tap open the file right away, before the list catches up with its new local path.
            is OfflineDownloadEvent.FileSaved -> sharedPathCache.recordCompletedPath(event.uuid, event.localPath)
            is OfflineDownloadEvent.RequestFinished -> {
                val isBulk = requests.remove(event.requestId) ?: return
                onRequestFinished(event.result, isBulk, host)
            }
        }
    }

    private fun onRequestFinished(result: BulkOfflineProgress.Result, isBulk: Boolean, host: CellScreenHost) {
        when (result) {
            is BulkOfflineProgress.Result.Saved ->
                host.sendAction(if (isBulk) ShowOfflineFilesSaved(result.savedCount) else ShowOfflineFileSaved)

            is BulkOfflineProgress.Result.Failed -> host.sendAction(ShowError(result.error))
            is BulkOfflineProgress.Result.RanOutOfStorage -> if (isBulk) {
                _bulkDialog.value = BulkOfflineDialog.RanOutOfStorage(savedCount = result.savedCount, totalCount = result.totalCount)
            } else {
                host.sendAction(ShowError(CellError.NO_SPACE_LEFT))
            }
        }
    }

    /** Removes the offline record of [file] and its copy on the device. */
    private suspend fun deleteOfflineCopy(file: CellNodeUi.File) {
        val localPath = getOfflineFile(file.uuid)?.localPath
            ?: file.localPath
            ?: sharedPathCache.getCompletedPath(file.uuid)

        // Remove the DB record so the UI stops showing the offline indicator.
        deleteOfflineFile(file.uuid)

        // Delete the physical file from device storage
        localPath?.takeIf { it.isNotBlank() }?.let { path ->
            withContext(Dispatchers.IO) { File(path).delete() }
        }

        sharedPathCache.clearCompletedPath(file.uuid)
        sharedPathCache.clearOpenLoadState(file.uuid)
    }

    private companion object {
        // Kept free on top of the files to download, so saving them doesn't fill the device up completely.
        const val STORAGE_SAFETY_MARGIN_BYTES = 50L * 1024 * 1024
    }
}
