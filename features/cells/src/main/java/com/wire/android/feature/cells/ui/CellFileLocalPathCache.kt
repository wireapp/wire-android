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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton shared state for the Cells file-open feature.
 *
 * Centralised here so that `CellViewModel` and `SearchScreenViewModel` share the same
 * reactive state without any UI-layer wiring.
 *
 * - [fileReadyEvents]: emitted when a slow download finishes so the UI can show a snackbar.
 * - [openLoadStates]: per-uuid Loading / Ready / Error state consumed by paging combines.
 * - [downloadProgresses]: per-uuid offline-download progress
 */
@Singleton
class CellFileLocalPathCache @Inject constructor() {

    private val _fileReadyChannel = Channel<CellNodeUi.File>(Channel.BUFFERED)
    val fileReadyEvents: Flow<CellNodeUi.File> = _fileReadyChannel.receiveAsFlow()

    private val _openLoadStates = MutableStateFlow<Map<String, OpenLoadState>>(emptyMap())
    internal val openLoadStates: StateFlow<Map<String, OpenLoadState>> = _openLoadStates.asStateFlow()

    private val _downloadProgresses = MutableStateFlow<Map<String, Float?>>(emptyMap())
    internal val downloadProgresses: StateFlow<Map<String, Float?>> = _downloadProgresses.asStateFlow()

    // Session-level guard: records the local path once a download completes so that a
    // subsequent tap opens the file immediately, even if the paging source hasn't refreshed
    // yet with the new localPath from the DB.
    private val completedPaths = mutableMapOf<String, String>()
    internal fun recordCompletedPath(uuid: String, path: String) {
        completedPaths[uuid] = path
    }

    internal fun getCompletedPath(uuid: String): String? = completedPaths[uuid]

    internal fun clearCompletedPath(uuid: String) {
        completedPaths.remove(uuid)
    }

    fun emitFileReady(file: CellNodeUi.File) {
        _fileReadyChannel.trySend(file)
    }

    internal fun setOpenLoadState(uuid: String, state: OpenLoadState) =
        _openLoadStates.update { it + (uuid to state) }

    internal fun clearOpenLoadState(uuid: String) = _openLoadStates.update { it - uuid }

    internal fun setDownloadProgress(uuid: String, progress: Float?) =
        _downloadProgresses.update { it + (uuid to progress) }

    internal fun clearDownloadProgress(uuid: String) =
        _downloadProgresses.update { it - uuid }
}
