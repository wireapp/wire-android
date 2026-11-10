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

/** A file waiting in [BulkOfflineDownloadQueue], with what to tell its request once its download settled. */
internal class QueuedOfflineDownload(
    val file: CellNodeUi.File,
    val onSaved: (localPath: String) -> Unit,
    val onFailed: (CellError) -> Unit,
    val onCancelled: () -> Unit,
)

/**
 * Throttles the offline downloads of one account, whichever request they come from (single file or bulk):
 * - files larger than [largeFileThresholdBytes] go first, one at a time, with nothing else downloading alongside;
 * - the other files download at most [maxConcurrentDownloads] at a time.
 *
 * [startDownload] must start the download of the given file and call `onSettled` exactly once when it ends,
 * whether it succeeded, failed or got cancelled, so the next queued file can start.
 *
 * Files waiting for their turn are flagged as queued in [pathCache] so the list can show it; [cancel] takes one out
 * of the queue and reports it through [QueuedOfflineDownload.onCancelled].
 */
internal class BulkOfflineDownloadQueue(
    files: List<CellNodeUi.File>,
    private val pathCache: CellFileLocalPathCache,
    private val maxConcurrentDownloads: Int = MAX_CONCURRENT_DOWNLOADS,
    private val largeFileThresholdBytes: Long = LARGE_FILE_THRESHOLD_BYTES,
    private val startDownload: (download: QueuedOfflineDownload, onSettled: () -> Unit) -> Unit,
) {
    private val pending = ArrayDeque<QueuedOfflineDownload>()
    private var runningCount = 0
    private var isLargeFileRunning = false

    /**
     * Adds [downloads] to the queue: large files go after the large files already waiting but before every small one,
     * the order is kept otherwise.
     */
    @Synchronized
    fun enqueue(downloads: List<QueuedOfflineDownload>) {
        if (downloads.isEmpty()) return
        pathCache.setDownloadsQueued(downloads.map { it.file.uuid })
        val (large, small) = downloads.partition { it.file.isLarge() }
        val firstSmallIndex = pending.indexOfFirst { !it.file.isLarge() }.takeIf { it >= 0 } ?: pending.size
        pending.addAll(firstSmallIndex, large)
        pending.addAll(small)
        startNextDownloads()
    }

    /** True when [uuid] is waiting for its turn. */
    @Synchronized
    fun contains(uuid: String): Boolean = pending.any { it.file.uuid == uuid }

    /** Takes [uuid] out of the queue. Returns false when it isn't waiting in the queue (already started or unknown). */
    @Synchronized
    fun cancel(uuid: String): Boolean {
        val download = pending.firstOrNull { it.file.uuid == uuid } ?: return false
        pending.remove(download)
        pathCache.clearDownloadsQueued(listOf(uuid))
        download.onCancelled()
        // A cancelled large file at the head of the queue may have been holding the others back.
        startNextDownloads()
        return true
    }

    /** Cancels every file still waiting, reporting each through [QueuedOfflineDownload.onCancelled]. */
    @Synchronized
    fun cancelPending() {
        val cancelled = pending.toList()
        pending.clear()
        pathCache.clearDownloadsQueued(cancelled.map { it.file.uuid })
        cancelled.forEach { it.onCancelled() }
    }

    @Synchronized
    private fun startNextDownloads() {
        while (pending.isNotEmpty()) {
            val next = pending.first()
            val canStart = if (next.file.isLarge()) {
                runningCount == 0
            } else {
                !isLargeFileRunning && runningCount < maxConcurrentDownloads
            }
            if (!canStart) return

            pending.removeFirst()
            runningCount++
            if (next.file.isLarge()) isLargeFileRunning = true
            startDownload(next, onSettled(next.file))
            // Only now: the download flagged the file as downloading, so it never shows up as idle in between.
            pathCache.clearDownloadsQueued(listOf(next.file.uuid))
        }
    }

    private fun onSettled(file: CellNodeUi.File): () -> Unit {
        var settled = false
        return {
            synchronized(this) {
                // Guards against a download reporting more than once, which would let too many run at once.
                if (!settled) {
                    settled = true
                    runningCount--
                    if (file.isLarge()) isLargeFileRunning = false
                    startNextDownloads()
                }
            }
        }
    }

    private fun CellNodeUi.File.isLarge(): Boolean = (size ?: 0) > largeFileThresholdBytes

    private companion object {
        const val MAX_CONCURRENT_DOWNLOADS = 3
        const val LARGE_FILE_THRESHOLD_BYTES = 100L * 1024 * 1024
    }
}
