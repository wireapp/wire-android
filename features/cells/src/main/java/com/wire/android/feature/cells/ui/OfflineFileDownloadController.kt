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

import com.wire.android.di.CurrentAccount
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.cellFileDestination
import com.wire.kalium.cells.domain.usecase.download.CellDownloadPausedException
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.cells.domain.usecase.offline.SaveOfflineFileUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import dev.zacsweers.metro.Inject

/**
 * Controller that handles downloading a cell file to app-specific external storage
 * and persisting its metadata in the database for offline access.
 */
class OfflineFileDownloadController @Inject constructor(
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val saveOfflineFile: SaveOfflineFileUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
    @CurrentAccount private val userId: UserId,
) {

    internal val downloadProgresses: StateFlow<Map<String, Float?>> = sharedPathCache.downloadProgresses

    private data class ActiveDownload(
        val job: Job,
        val filePath: Path,
        val onCancel: () -> Unit,
        val onError: (CellError) -> Unit,
    )
    private val activeJobs = mutableMapOf<String, ActiveDownload>()

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onSuccess: (localPath: String) -> Unit,
        onError: (CellError) -> Unit,
        onCancel: () -> Unit = {},
        initialProgress: Float? = null,
    ) {
        // If the file already exists locally (loaded this session or stored in DB),
        // skip the download and just persist the offline metadata.
        val existingPath = cellNode.localPath ?: sharedPathCache.getCompletedPath(cellNode.uuid)
        if (existingPath != null && File(existingPath).exists()) {
            saveExistingOfflineFile(scope, cellNode, existingPath, onSuccess, onError)
            return
        }

        // Cancel any previous download for this node.
        activeJobs.remove(cellNode.uuid)?.let { previous ->
            previous.job.cancel()
            previous.onCancel()
        }

        val nodeName = cellNode.name ?: run {
            onError(CellError.OTHER_ERROR)
            return
        }
        val filePath = fileHelper.cellFileDestination(userId, cellNode.conversationId, cellNode.uuid, nodeName).toOkioPath()

        // Flag the file as downloading right away rather than once the coroutine runs, so a file leaving the
        // bulk queue goes straight from queued to downloading, see BulkOfflineDownloadQueue. A resumed download starts
        // from where it paused.
        setProgress(cellNode.uuid, initialProgress)

        val job = scope.launch {
            performDownload(cellNode, nodeName, filePath, onSuccess, onError)
        }

        activeJobs[cellNode.uuid] = ActiveDownload(job, filePath, onCancel, onError)
        // Only if still this download: a paused one may already have been started again for the same file.
        job.invokeOnCompletion { if (activeJobs[cellNode.uuid]?.job === job) activeJobs.remove(cellNode.uuid) }
    }

    private fun saveExistingOfflineFile(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        existingPath: String,
        onSuccess: (localPath: String) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        val nodeName = cellNode.name ?: run {
            onError(CellError.OTHER_ERROR)
            return
        }
        scope.launch {
            saveOfflineFile(
                OfflineFileInfo(
                    id = cellNode.uuid,
                    name = nodeName,
                    mimeType = cellNode.mimeType,
                    owner = cellNode.ownerUserId ?: "",
                    localPath = existingPath,
                    size = cellNode.size,
                    downloadedAt = System.currentTimeMillis(),
                    conversationId = cellNode.conversationId,
                    modifiedAt = cellNode.modifiedTime,
                    remotePath = cellNode.remotePath,
                )
            )
            onSuccess(existingPath)
        }
    }

    private suspend fun CoroutineScope.performDownload(
        cellNode: CellNodeUi.File,
        nodeName: String,
        filePath: Path,
        onSuccess: (localPath: String) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        val thisJob = coroutineContext.job

        val result = download(
            assetId = cellNode.uuid,
            conversationId = cellNode.conversationId,
            outFilePath = filePath,
            remoteFilePath = cellNode.remotePath,
            assetSize = cellNode.size ?: 0,
            name = cellNode.name,
            ownerId = cellNode.ownerUserId,
        ) { progress ->
            if (thisJob.isActive) {
                val assetSize = cellNode.size ?: 0
                if (assetSize > 0) {
                    val progressValue = (progress.toFloat() / assetSize).coerceIn(0f, 1f)
                    setProgress(cellNode.uuid, progressValue)
                }
            }
        }

        result.onSuccess {
            clearProgress(cellNode.uuid)
            sharedPathCache.recordCompletedPath(cellNode.uuid, filePath.toString())
            saveOfflineFile(
                OfflineFileInfo(
                    id = cellNode.uuid,
                    name = nodeName,
                    mimeType = cellNode.mimeType,
                    owner = cellNode.ownerUserId ?: "",
                    localPath = filePath.toString(),
                    size = cellNode.size,
                    downloadedAt = System.currentTimeMillis(),
                    conversationId = cellNode.conversationId,
                    modifiedAt = cellNode.modifiedTime,
                    remotePath = cellNode.remotePath,
                )
            )
            onSuccess(filePath.toString())
        }

        if (result is Either.Left) {
            if (result.value is NetworkFailure.NoNetworkConnection) {
                // Paused rather than failed: what was downloaded so far stays in its `.part` file to resume from, and the
                // progress stays too, for the caller to show where it paused (see CellFileLocalPathCache.pauseDownload).
                onError(CellError.NO_NETWORK)
                return
            }
            clearProgress(cellNode.uuid)
            // Fire-and-forget delete so the error callback is not blocked by IO.
            launch(Dispatchers.IO) { File(filePath.toString()).delete() }
            onError(if (result.value.isNoSpaceLeft()) CellError.NO_SPACE_LEFT else CellError.DOWNLOAD_FAILED)
        }
    }

    internal fun isDownloading(uuid: String): Boolean = activeJobs.containsKey(uuid)

    /**
     * Stops every running download because the connection dropped, without waiting for their requests to time out:
     * each reports [CellError.NO_NETWORK], keeps its progress, and keeps what it downloaded so far to resume from.
     */
    internal fun pauseAll() {
        val paused = activeJobs.values.toList()
        activeJobs.clear()
        paused.forEach { active ->
            active.job.cancel(CellDownloadPausedException())
            active.onError(CellError.NO_NETWORK)
        }
    }

    internal fun cancelAll(scope: CoroutineScope) {
        activeJobs.keys.toList().forEach { cancel(it, scope) }
    }

    /** Free space left where offline files are stored, see [cellFileDestination]. */
    internal fun availableStorageBytes(): Long = fileHelper.getExternalFilesDir().usableSpace

    internal fun cancel(uuid: String, scope: CoroutineScope) {
        val active = activeJobs.remove(uuid) ?: return
        active.job.cancel()
        active.onCancel()
        clearProgress(uuid)
        // Delete the partial file left by the cancelled download.
        scope.launch(Dispatchers.IO) { File(active.filePath.toString()).delete() }
    }

    private fun setProgress(uuid: String, progress: Float?) {
        sharedPathCache.setDownloadProgress(uuid, progress)
    }

    private fun clearProgress(uuid: String) {
        sharedPathCache.clearDownloadProgress(uuid)
    }
}
