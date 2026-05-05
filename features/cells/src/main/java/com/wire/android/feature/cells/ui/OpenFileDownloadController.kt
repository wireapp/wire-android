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

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/**
 * Controller for downloading a Cell file when the user clicks "Open". Manages the download lifecycle
 * and exposes loading state to the UI.
 *
 * Responsibilities:
 * - Start a download when the user clicks "Open" on a file with an unknown local path.
 * - Expose per-file loading state (Loading / Ready / Error) via [CellFileLocalPathCache.openLoadStates].
 * - Cancel in-progress downloads if the user clicks "Open" again on the same file or navigates away.
 *
 * The UI combines [openLoadStates] with the existing CellNodeUi to show a spinner, progress, and error state.
 * When a download finishes, the controller updates the state to show a "Ready" badge and emits a one-time
 * event so the UI can show a snackbar with an "Open" action.
 */
class OpenFileDownloadController @Inject constructor(
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val fileNameResolver: FileNameResolver,
    private val sharedPathCache: CellFileLocalPathCache,
) {
    // Active download jobs keyed by asset uuid
    private val activeDownloads = mutableMapOf<String, Job>()

    internal val openLoadStates: StateFlow<Map<String, OpenLoadState>> = sharedPathCache.openLoadStates

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onOpenFile: (CellNodeUi.File) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        // Open immediately if the path is already known
        if (cellNode.localPath != null) {
            onOpenFile(cellNode)
            return
        }

        // Cancel any in-progress download for this file
        activeDownloads.remove(cellNode.uuid)?.cancel()

        activeDownloads[cellNode.uuid] = scope.launch {
            val nodeName = cellNode.name ?: run {
                onError(CellError.OTHER_ERROR)
                return@launch
            }

            val filePath = fileNameResolver
                .getUniqueFile(fileHelper.getCacheDir(), nodeName)
                .toPath()
                .toOkioPath()

            // After 300 ms show the spinner. Cancelled immediately if the download finishes first.
            val showSpinnerJob = launch {
                delay(SPINNER_THRESHOLD_MS)
                sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading())
            }

            download(
                assetId = cellNode.uuid,
                outFilePath = filePath,
                remoteFilePath = cellNode.remotePath,
                assetSize = cellNode.size ?: 0,
            ) { bytesDownloaded ->
                // Only emit progress updates after the spinner threshold has been crossed.
                if (sharedPathCache.openLoadStates.value.containsKey(cellNode.uuid)) {
                    launch {
                        val total = cellNode.size ?: 0
                        if (total > 0) {
                            val progress = (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
                            sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading(progress))
                        }
                    }
                }
            }.onSuccess {
                val pathStr = filePath.toString()
                val spinnerWasShown = sharedPathCache.openLoadStates.value.containsKey(cellNode.uuid)
                showSpinnerJob.cancel()
                activeDownloads -= cellNode.uuid

                if (!spinnerWasShown) {
                    // Fast path (<300 ms): open immediately with no state change
                    onOpenFile(cellNode.copy(localPath = pathStr))
                } else {
                    // Slow path: user saw the spinner — show "Ready" badge + snackbar.
                    sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Ready(filePath))
                    sharedPathCache.emitFileReady(cellNode.copy(localPath = pathStr))
                    launch {
                        delay(READY_BADGE_DISMISS_MS)
                        sharedPathCache.clearOpenLoadState(cellNode.uuid)
                    }
                }
            }.onFailure {
                showSpinnerJob.cancel()
                activeDownloads -= cellNode.uuid
                sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Error)
            }
        }
    }

    internal fun cancel(uuid: String) {
        activeDownloads.remove(uuid)?.cancel()
        sharedPathCache.clearOpenLoadState(uuid)
    }

    companion object {
        private const val SPINNER_THRESHOLD_MS = 400L
        private const val READY_BADGE_DISMISS_MS = 3_000L
    }
}
