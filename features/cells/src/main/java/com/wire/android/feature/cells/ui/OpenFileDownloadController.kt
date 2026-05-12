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

import com.wire.android.feature.cells.ui.OpenFileDownloadController.Companion.SPINNER_THRESHOLD_MS
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
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
    private data class ActiveDownload(val job: Job, val filePath: Path)

    private val activeDownloads = mutableMapOf<String, ActiveDownload>()

    internal val openLoadStates = sharedPathCache.openLoadStates

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
        activeDownloads.remove(cellNode.uuid)?.job?.cancel()

        val nodeName = cellNode.name ?: run {
            onError(CellError.OTHER_ERROR)
            return
        }

        val filePath = fileNameResolver
            .getUniqueFile(fileHelper.getExternalFilesDir(), nodeName)
            .toPath()
            .toOkioPath()

        val job = scope.launch {
            performDownload(cellNode, filePath, onOpenFile, onError)
        }
        activeDownloads[cellNode.uuid] = ActiveDownload(job, filePath)
    }

    private suspend fun CoroutineScope.performDownload(
        cellNode: CellNodeUi.File,
        filePath: Path,
        onOpenFile: (CellNodeUi.File) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        val thisJob = coroutineContext.job

        // After SPINNER_THRESHOLD_MS show the spinner. Cancelled immediately if the download finishes first.
        val showSpinnerJob = launch {
            delay(SPINNER_THRESHOLD_MS)
            sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading())
        }

        val result = download(
            assetId = cellNode.uuid,
            outFilePath = filePath,
            remoteFilePath = cellNode.remotePath,
            assetSize = cellNode.size ?: 0,
        ) { bytesDownloaded ->
            if (thisJob.isActive &&
                sharedPathCache.openLoadStates.value.containsKey(cellNode.uuid)
            ) {
                val total = cellNode.size ?: 0
                if (total > 0) {
                    val progress = (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
                    sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Loading(progress))
                }
            }
        }

        result.onSuccess {
            val pathStr = filePath.toString()
            sharedPathCache.recordCompletedPath(cellNode.uuid, pathStr)
            val spinnerWasShown = sharedPathCache.openLoadStates.value.containsKey(cellNode.uuid)
            showSpinnerJob.cancel()
            activeDownloads.remove(cellNode.uuid)

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

        if (result is Either.Left) {
            showSpinnerJob.cancel()
            activeDownloads.remove(cellNode.uuid)

            launch(Dispatchers.IO) { File(filePath.toString()).delete() }
            if (result.value.isNoSpaceLeft()) {
                sharedPathCache.clearOpenLoadState(cellNode.uuid)
                onError(CellError.NO_SPACE_LEFT)
            } else {
                sharedPathCache.setOpenLoadState(cellNode.uuid, OpenLoadState.Error)
            }
        }
    }

    internal fun cancel(uuid: String, scope: CoroutineScope) {
        val active = activeDownloads.remove(uuid) ?: return
        active.job.cancel()
        sharedPathCache.clearOpenLoadState(uuid)
        // Delete the partial file left by the cancelled download.
        scope.launch(Dispatchers.IO) { File(active.filePath.toString()).delete() }
    }

    companion object {
        internal const val SPINNER_THRESHOLD_MS = 400L
        private const val READY_BADGE_DISMISS_MS = 3_000L
    }
}
