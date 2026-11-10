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
package com.wire.android.feature.cells.ui.offline

/**
 * Where the offline downloads of an account stand since it last had none running, what the ongoing notification shows.
 * Cancelled files are not counted.
 *
 * @param pendingCount files still downloading or waiting for their turn.
 * @param totalBytes the summed size of the files whose size is known, 0 when none is.
 * @param ranOutOfStorage a download failed because the device was full.
 */
data class OfflineDownloadSummary(
    val totalCount: Int = 0,
    val savedCount: Int = 0,
    val failedCount: Int = 0,
    val pendingCount: Int = 0,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val ranOutOfStorage: Boolean = false,
) {
    val isActive: Boolean get() = pendingCount > 0

    operator fun plus(other: OfflineDownloadSummary) = OfflineDownloadSummary(
        totalCount = totalCount + other.totalCount,
        savedCount = savedCount + other.savedCount,
        failedCount = failedCount + other.failedCount,
        pendingCount = pendingCount + other.pendingCount,
        totalBytes = totalBytes + other.totalBytes,
        downloadedBytes = downloadedBytes + other.downloadedBytes,
        ranOutOfStorage = ranOutOfStorage || other.ranOutOfStorage,
    )
}
