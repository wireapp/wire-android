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
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.cells.domain.usecase.offline.SaveOfflineFileUseCase
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File
import javax.inject.Inject

/**
 * Controller that handles downloading a cell file to app-specific external storage
 * and persisting its metadata in the database for offline access.
 */
class OfflineFileDownloadController @Inject constructor(
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val fileNameResolver: FileNameResolver,
    private val saveOfflineFile: SaveOfflineFileUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) {

    internal val downloadProgresses: StateFlow<Map<String, Float?>> = sharedPathCache.downloadProgresses

    private data class ActiveDownload(val job: Job, val filePath: Path)
    private val activeJobs = mutableMapOf<String, ActiveDownload>()

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onSuccess: (localPath: String) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        // If the file already exists locally (loaded this session or stored in DB),
        // skip the download and just persist the offline metadata.
        val existingPath = cellNode.localPath ?: sharedPathCache.getCompletedPath(cellNode.uuid)
        if (existingPath != null) {
            saveExistingOfflineFile(scope, cellNode, existingPath, onSuccess, onError)
            return
        }

        // Cancel any previous download for this node.
        activeJobs.remove(cellNode.uuid)?.job?.cancel()

        val nodeName = cellNode.name ?: run {
            onError(CellError.OTHER_ERROR)
            return
        }
        val filePath = fileNameResolver
            .getUniqueFile(fileHelper.getExternalFilesDir(), nodeName)
            .toPath()
            .toOkioPath()

        val job = scope.launch {
            performDownload(cellNode, nodeName, filePath, onSuccess, onError)
        }

        activeJobs[cellNode.uuid] = ActiveDownload(job, filePath)
        job.invokeOnCompletion { activeJobs.remove(cellNode.uuid) }
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
        setProgress(cellNode.uuid, null)

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
                )
            )
            onSuccess(filePath.toString())
        }

        if (result is Either.Left) {
            clearProgress(cellNode.uuid)
            // Fire-and-forget delete so the error callback is not blocked by IO.
            launch(Dispatchers.IO) { File(filePath.toString()).delete() }
            onError(if (result.value.isNoSpaceLeft()) CellError.NO_SPACE_LEFT else CellError.DOWNLOAD_FAILED)
        }
    }

    internal fun cancel(uuid: String, scope: CoroutineScope) {
        val active = activeJobs.remove(uuid) ?: return
        active.job.cancel()
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
