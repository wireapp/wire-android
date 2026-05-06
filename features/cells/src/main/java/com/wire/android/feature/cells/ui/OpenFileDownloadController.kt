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
 * Controller for managing the state of file downloads triggered by "Open" actions in the UI.
 */
class OpenFileDownloadController @Inject constructor(
    private val download: DownloadCellFileUseCase,
    private val fileHelper: FileHelper,
    private val fileNameResolver: FileNameResolver,
    private val sharedPathCache: CellFileLocalPathCache,
) {

    private val _openDownloads = MutableStateFlow<Map<String, Job>>(emptyMap())

    private val _openLoadStates = MutableStateFlow<Map<String, OpenLoadState>>(emptyMap())

    internal val openLoadStates: StateFlow<Map<String, OpenLoadState>> = _openLoadStates.asStateFlow()

    internal fun start(
        scope: CoroutineScope,
        cellNode: CellNodeUi.File,
        onOpenFile: (CellNodeUi.File) -> Unit,
        onError: (CellError) -> Unit,
    ) {
        // Cancel any previous download for this file (e.g. rapid retries after error).
        _openDownloads.value[cellNode.uuid]?.cancel()

        val job = scope.launch {
            val nodeName = cellNode.name ?: run {
                onError(CellError.OTHER_ERROR)
                return@launch
            }

            val cacheDir = fileHelper.getCacheDir()
            val filePath = fileNameResolver.getUniqueFile(cacheDir, nodeName).toPath().toOkioPath()

            var spinnerShown = false

            val showSpinnerJob = launch {
                delay(OPEN_SPINNER_DELAY_MS)
                spinnerShown = true
                setLoadState(cellNode.uuid, OpenLoadState.Loading())
            }

            download(
                assetId = cellNode.uuid,
                outFilePath = filePath,
                remoteFilePath = cellNode.remotePath,
                assetSize = cellNode.size ?: 0,
                name = cellNode.name,
                ownerId = cellNode.ownerUserId,
            ) { progress ->
                // Child coroutine — cancelled automatically when the parent job is cancelled,
                launch {
                    val assetSize = cellNode.size ?: 0
                    if (assetSize > 0) {
                        val progressValue = (progress.toFloat() / assetSize).coerceIn(0f, 1f)
                        setLoadState(cellNode.uuid, OpenLoadState.Loading(progressValue))
                    }
                }
            }
                .onSuccess {
                    showSpinnerJob.cancel()
                    _openDownloads.update { it - cellNode.uuid }
                    sharedPathCache.put(cellNode.uuid, filePath.toString())

                    if (!spinnerShown) {
                        // Fast path: completed before threshold — open immediately.
                        clearLoadState(cellNode.uuid)
                        onOpenFile(cellNode.copy(localPath = filePath.toString()))
                    } else {
                        // Slow path: spinner was visible — show "Ready" badge + snackbar.
                        setLoadState(cellNode.uuid, OpenLoadState.Ready(filePath))
                        sharedPathCache.emitFileReady(cellNode.copy(localPath = filePath.toString()))
                        launch {
                            delay(OPEN_READY_DISMISS_MS)
                            clearLoadState(cellNode.uuid)
                        }
                    }
                }
                .onFailure {
                    showSpinnerJob.cancel()
                    _openDownloads.update { it - cellNode.uuid }
                    setLoadState(cellNode.uuid, OpenLoadState.Error)
                }
        }

        _openDownloads.update { it + (cellNode.uuid to job) }
    }

    internal fun cancel(uuid: String) {
        var job: Job? = null
        _openDownloads.update { map ->
            job = map[uuid]
            map - uuid
        }
        job?.cancel()
        clearLoadState(uuid)
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
        private const val OPEN_SPINNER_DELAY_MS = 300L
        private const val OPEN_READY_DISMISS_MS = 3_000L
    }
}
