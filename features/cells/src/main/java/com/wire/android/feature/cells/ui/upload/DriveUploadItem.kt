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
package com.wire.android.feature.cells.ui.upload

import okio.Path

/**
 * A file picked by the user for a direct upload into a Shared Drive folder.
 *
 * @param localPath staged local copy of the picked file, which has to stay readable until the upload ends
 * @param destinationFolderPath Shared Drive folder receiving the file, as `cellName[/subFolder...]`
 */
data class DriveUploadRequest(
    val localPath: Path,
    val fileName: String,
    val sizeBytes: Long,
    val destinationFolderPath: String,
)

sealed interface DriveUploadState {
    data object Queued : DriveUploadState
    data class Uploading(val progress: Float = 0f) : DriveUploadState
    data object Completed : DriveUploadState
    data object Failed : DriveUploadState
    data object Cancelled : DriveUploadState
}

/**
 * A single upload tracked by [DriveUploadCoordinator]: the picked file plus its current lifecycle state.
 */
data class DriveUploadItem(
    val id: String,
    val request: DriveUploadRequest,
    val state: DriveUploadState = DriveUploadState.Queued,
    /** Draft node created by `CellUploadManager` once the transfer starts, needed to cancel or retry it. */
    val nodeUuid: String? = null,
) {
    val fileName: String get() = request.fileName
    val sizeBytes: Long get() = request.sizeBytes
}
