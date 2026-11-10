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

/**
 * Throttles the downloads of one "make available offline" bulk action:
 * - files larger than [largeFileThresholdBytes] go first, one at a time, with nothing else downloading alongside;
 * - the other files then download at most [maxConcurrentDownloads] at a time.
 *
 * [startDownload] must start the download of the given file and call `onSettled` exactly once when it ends,
 * whether it succeeded, failed or got cancelled, so the next queued file can start.
 *
 * Files waiting for their turn are flagged as queued in [pathCache] so the list can show it; [cancel] takes one out
 * of the queue and reports it through [onQueuedFileCancelled].
 */
internal class BulkOfflineDownloadQueue(
    files: List<CellNodeUi.File>,
    private val pathCache: CellFileLocalPathCache,
    private val maxConcurrentDownloads: Int = MAX_CONCURRENT_DOWNLOADS,
    private val largeFileThresholdBytes: Long = LARGE_FILE_THRESHOLD_BYTES,
    private val onQueuedFileCancelled: (file: CellNodeUi.File) -> Unit,
    private val startDownload: (file: CellNodeUi.File, onSettled: () -> Unit) -> Unit,
) {
    // Stable sort: large files first, the selection order is kept otherwise.
    private val pending = ArrayDeque(files.sortedByDescending { it.isLarge() })
    private var runningCount = 0
    private var isLargeFileRunning = false

    /** True once every file was started or cancelled; the downloads themselves may still be running. */
    val isDrained: Boolean
        @Synchronized get() = pending.isEmpty()

    fun start() {
        pathCache.setDownloadsQueued(pending.map { it.uuid })
        startNextDownloads()
    }

    /** Takes [uuid] out of the queue. Returns false when it isn't waiting in this queue (already started or unknown). */
    @Synchronized
    fun cancel(uuid: String): Boolean {
        val file = pending.firstOrNull { it.uuid == uuid } ?: return false
        pending.remove(file)
        pathCache.clearDownloadsQueued(listOf(uuid))
        onQueuedFileCancelled(file)
        // A cancelled large file at the head of the queue may have been holding the others back.
        startNextDownloads()
        return true
    }

    /** Cancels every file still waiting, reporting each through [onQueuedFileCancelled]. */
    @Synchronized
    fun cancelPending() {
        val cancelled = pending.toList()
        pending.clear()
        pathCache.clearDownloadsQueued(cancelled.map { it.uuid })
        cancelled.forEach(onQueuedFileCancelled)
    }

    /** Drops every file still waiting, without reporting them: used when the screen goes away. */
    @Synchronized
    fun clear() {
        pathCache.clearDownloadsQueued(pending.map { it.uuid })
        pending.clear()
    }

    @Synchronized
    private fun startNextDownloads() {
        while (pending.isNotEmpty()) {
            val next = pending.first()
            val canStart = if (next.isLarge()) {
                runningCount == 0
            } else {
                !isLargeFileRunning && runningCount < maxConcurrentDownloads
            }
            if (!canStart) return

            pending.removeFirst()
            runningCount++
            if (next.isLarge()) isLargeFileRunning = true
            startDownload(next, onSettled(next))
            // Only now: the download flagged the file as downloading, so it never shows up as idle in between.
            pathCache.clearDownloadsQueued(listOf(next.uuid))
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
