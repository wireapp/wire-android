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
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

/**
 * Controller responsible for managing the download and open flow for cell files.
 *
 * When a file is opened, this controller checks if the local path is already known. If not, it
 * initiates a download using [DownloadCellFileUseCase]. If the download takes longer than
 * [SPINNER_THRESHOLD_MS], it updates the [sharedPathCache] to show a loading spinner in the UI.
 * Once the download completes, it updates the cache with either a "Ready" state (if the spinner
 * was shown) or opens the file immediately (if the download was fast). It also handles cancellation
 * of in-progress downloads and error states.
 */
class OpenFileDownloadController @Inject constructor(
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val fileNameResolver: FileNameResolver,
    private val sharedPathCache: CellFileLocalPathCache,
) {
    // Active download jobs keyed by asset uuid. All access is from viewModelScope (main thread).
    private val activeDownloads = mutableMapOf<String, Job>()

    private val _openDownloads = MutableStateFlow<Map<String, Job>>(emptyMap())

    private val _openLoadStates = MutableStateFlow<Map<String, OpenLoadState>>(emptyMap())

    internal val openLoadStates: StateFlow<Map<String, OpenLoadState>> = _openLoadStates.asStateFlow()

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onOpenFile: (CellNodeUi.File) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        // Open immediately if the path is already known — either persisted in the DB
        // (node.localPath) or recorded in this session's completed-paths guard (covers the
        // window between download completion and paging source refresh).
        val knownPath = cellNode.localPath ?: sharedPathCache.getCompletedPath(cellNode.uuid)
        if (knownPath != null) {
            onOpenFile(cellNode.copy(localPath = knownPath))
            return
        }

        // Cancel any in-progress download for this file (e.g. rapid retries after an error).
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

            // After SPINNER_THRESHOLD_MS show the spinner. Cancelled immediately if the download finishes first.
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
                    val total = cellNode.size ?: 0
                    if (total > 0) {
                        val progress = (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
                        sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading(progress))
                    }
                }
            }
                .onSuccess {
                    val pathStr = filePath.toString()
                    // Record in session guard so repeat taps open immediately even if the
                    // paging source hasn't refreshed yet with the new localPath from the DB.
                    sharedPathCache.recordCompletedPath(cellNode.uuid, pathStr)
                    val spinnerWasShown = sharedPathCache.openLoadStates.value.containsKey(cellNode.uuid)
                    showSpinnerJob.cancel()
                    activeDownloads -= cellNode.uuid

                    if (!spinnerWasShown) {
                        // Fast path (<SPINNER_THRESHOLD_MS): open immediately with no state change → no list animation.
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
                }
                .onFailure {
                    showSpinnerJob.cancel()
                    activeDownloads -= cellNode.uuid
                    sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Error)
                }
        }

        _openDownloads.update { it + (cellNode.uuid to job) }
    }

    internal fun cancel(uuid: String) {
        activeDownloads.remove(uuid)?.cancel()
        sharedPathCache.clearOpenLoadState(uuid)
    }

    internal fun clearAllErrorStates() =
        _openLoadStates.update { states ->
            states.filterValues { it !is OpenLoadState.Error }.toImmutableMap()
        }

    private fun setLoadState(uuid: String, state: OpenLoadState) =
        _openLoadStates.update { it.toMutableMap().apply { put(uuid, state) }.toImmutableMap() }

    private fun clearLoadState(uuid: String) =
        _openLoadStates.update { it.toMutableMap().apply { remove(uuid) }.toImmutableMap() }

    companion object {
        private const val SPINNER_THRESHOLD_MS = 400L
        private const val READY_BADGE_DISMISS_MS = 3_000L
    }
}
