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
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
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
) {
    private val _downloadProgresses = MutableStateFlow<Map<String, Float?>>(emptyMap())
    private val _activeJobs = MutableStateFlow<Map<String, Job>>(emptyMap())

    /** Maps uuid → download progress (0f–1f) for all active offline downloads. */
    internal val downloadProgresses: StateFlow<Map<String, Float?>> = _downloadProgresses.asStateFlow()

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onSuccess: (localPath: String) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        // Cancel any previous download for this node
        _activeJobs.value[cellNode.uuid]?.cancel()

        val job = scope.launch {
            val nodeName = cellNode.name ?: run {
                onError(CellError.OTHER_ERROR)
                return@launch
            }

            val externalDir = fileHelper.getExternalFilesDir()
            val filePath = fileNameResolver.getUniqueFile(externalDir, nodeName).toPath().toOkioPath()

            _downloadProgresses.update { it.toMutableMap().apply { put(cellNode.uuid, null) }.toImmutableMap() }

            download(
                assetId = cellNode.uuid,
                outFilePath = filePath,
                remoteFilePath = cellNode.remotePath,
                assetSize = cellNode.size ?: 0,
                name = cellNode.name,
                ownerId = cellNode.ownerUserId,
            ) { progress ->
                launch {
                    val assetSize = cellNode.size ?: 0
                    if (assetSize > 0) {
                        val progressValue = (progress.toFloat() / assetSize).coerceIn(0f, 1f)
                        _downloadProgresses.update {
                            it.toMutableMap().apply { put(cellNode.uuid, progressValue) }.toImmutableMap()
                        }
                    }
                }
            }
                .onSuccess {
                    _downloadProgresses.update { it.toMutableMap().apply { remove(cellNode.uuid) }.toImmutableMap() }
                    saveOfflineFile(
                        OfflineFileInfo(
                            id = cellNode.uuid,
                            name = nodeName,
                            owner = cellNode.ownerUserId ?: "",
                            localPath = filePath.toString(),
                            size = cellNode.size,
                            downloadedAt = System.currentTimeMillis(),
                        )
                    )
                    onSuccess(filePath.toString())
                }
                .onFailure {
                    _downloadProgresses.update { it.toMutableMap().apply { remove(cellNode.uuid) }.toImmutableMap() }
                    onError(CellError.DOWNLOAD_FAILED)
                }
        }

        _activeJobs.update { it.toMutableMap().apply { put(cellNode.uuid, job) }.toImmutableMap() }
        job.invokeOnCompletion {
            _activeJobs.update { it.toMutableMap().apply { remove(cellNode.uuid) }.toImmutableMap() }
        }
    }

    internal fun cancel(uuid: String) {
        var job: Job? = null
        _activeJobs.update { current ->
            job = current[uuid]
            current.toMutableMap().apply { remove(uuid) }.toImmutableMap()
        }
        job?.cancel()
        _downloadProgresses.update { it.toMutableMap().apply { remove(uuid) }.toImmutableMap() }
    }
}
