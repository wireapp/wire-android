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

import android.net.Uri
import com.wire.android.feature.cells.ui.upload.DriveUploadFilePreparer
import com.wire.kalium.cells.domain.CellUploadCoordinator
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Uploads the files picked on a files screen into the folder it shows, once confirmed. One instance per screen. */
class CellUploadController @Inject constructor(
    private val uploadFilePreparer: DriveUploadFilePreparer,
    private val uploadCoordinator: CellUploadCoordinator,
) {

    private val _confirmation = MutableStateFlow<UploadConfirmation?>(null)
    internal val confirmation: StateFlow<UploadConfirmation?> = _confirmation.asStateFlow()

    private var pendingUris: List<Uri> = emptyList()

    /**
     * Resolves the display name of each picked [Uri] and surfaces [confirmation] so the screen can show the
     * confirmation dialog before anything is actually uploaded.
     */
    internal fun onFilesPicked(uris: List<Uri>, host: CellScreenHost) {
        if (uris.isEmpty()) return
        val destinationFolderPath = host.context.currentPath ?: return
        pendingUris = uris
        host.scope.launch {
            val fileNames = uris.map { uploadFilePreparer.fileName(it) ?: it.lastPathSegment ?: it.toString() }
            _confirmation.value = UploadConfirmation(fileNames, destinationFolderPath)
        }
    }

    /**
     * Registers every picked file as [com.wire.kalium.cells.domain.CellUploadState.Preparing] in the upload coordinator
     * immediately, so the status screen can open right away showing a real row per file rather than waiting on staging
     * (which can take a moment for large files) or showing an empty list that would read as "Upload complete" before
     * anything has actually started. Each file is then staged in turn: a successful one is attached to its placeholder
     * (moving it to Queued) right away, a failed one is dropped. Any file dropped from the batch during staging (whether
     * all of them or just some) is surfaced as an error instead of silently vanishing.
     */
    internal fun confirm(host: CellScreenHost) {
        val confirmation = _confirmation.value ?: return
        val uris = pendingUris
        cancel()

        val conversationId = confirmation.destinationFolderPath.substringBefore("/")
        val preparingIds = uris.indices.map { index ->
            uploadCoordinator.beginPreparing(confirmation.fileNames.getOrElse(index) { uris[index].toString() }, conversationId)
        }
        host.sendAction(FilesPickedForUpload(uris))
        host.scope.launch {
            var stagedCount = 0
            uris.forEachIndexed { index, uri ->
                val request = uploadFilePreparer.prepare(uri, confirmation.destinationFolderPath)
                if (request != null) {
                    stagedCount++
                    uploadCoordinator.attachPreparedRequest(preparingIds[index], request)
                } else {
                    uploadCoordinator.failPreparing(preparingIds[index])
                }
            }
            when {
                stagedCount == 0 -> host.sendAction(ShowError(CellError.UPLOAD_PREPARATION_FAILED))
                stagedCount < uris.size -> host.sendAction(ShowError(CellError.UPLOAD_PREPARATION_PARTIALLY_FAILED))
            }
        }
    }

    internal fun cancel() {
        _confirmation.value = null
        pendingUris = emptyList()
    }
}

internal data class UploadConfirmation(
    val fileNames: List<String>,
    val destinationFolderPath: String,
)
