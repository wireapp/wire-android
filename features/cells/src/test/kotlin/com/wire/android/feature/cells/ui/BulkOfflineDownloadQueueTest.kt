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
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.kalium.cells.domain.model.Node
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BulkOfflineDownloadQueueTest {

    private val pathCache = CellFileLocalPathCache()

    /** Uuids in the order their download started. */
    private val started = mutableListOf<String>()
    private val settleCallbacks = mutableMapOf<String, () -> Unit>()
    private val cancelled = mutableListOf<String>()

    private val queue = BulkOfflineDownloadQueue(
        pathCache = pathCache,
        maxConcurrentDownloads = MAX_CONCURRENT,
        largeFileThresholdBytes = LARGE_FILE_THRESHOLD,
    ) { download, onSettled ->
        started += download.file.uuid
        settleCallbacks[download.file.uuid] = onSettled
    }

    @Test
    fun `given more small files than allowed at once when enqueued then only the allowed number starts`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3")))

        assertEquals(listOf("1", "2"), started)
    }

    @Test
    fun `given the slots taken when a download settles then the next waiting file starts`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3")))

        settle("1")

        assertEquals(listOf("1", "2", "3"), started)
    }

    @Test
    fun `given a download reporting twice when it settles then only one slot is freed`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3"), small("4")))

        settle("1")
        settle("1")

        assertEquals(listOf("1", "2", "3"), started)
    }

    @Test
    fun `given small files running when a large file is enqueued then it waits for them to settle`() {
        queue.enqueue(downloads(small("1")))
        queue.enqueue(downloads(large("big")))

        assertEquals(listOf("1"), started)

        settle("1")

        assertEquals(listOf("1", "big"), started)
    }

    @Test
    fun `given a large file running when small files wait then they start only once it settled`() {
        queue.enqueue(downloads(large("big"), small("1"), small("2")))

        assertEquals(listOf("big"), started)

        settle("big")

        assertEquals(listOf("big", "1", "2"), started)
    }

    @Test
    fun `given small files waiting when large files are enqueued then the large files go first`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3"), small("4")))

        queue.enqueue(downloads(large("big")))
        settle("1")
        settle("2")

        assertEquals(listOf("1", "2", "big"), started)
    }

    @Test
    fun `given files waiting when enqueued then they are flagged as queued until they start`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3")))

        assertEquals(setOf("3"), pathCache.offlineDownloadStates.value.queued)

        settle("1")

        assertTrue(pathCache.offlineDownloadStates.value.queued.isEmpty())
    }

    @Test
    fun `given a file waiting when cancelled then it is reported cancelled and never starts`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3")))

        val wasWaiting = queue.cancel("3")
        settle("1")

        assertTrue(wasWaiting)
        assertEquals(listOf("3"), cancelled)
        assertEquals(listOf("1", "2"), started)
        assertFalse(queue.contains("3"))
        assertFalse("3" in pathCache.offlineDownloadStates.value.queued)
    }

    @Test
    fun `given a file already running when cancelled in the queue then it is left to the download itself`() {
        queue.enqueue(downloads(small("1")))

        assertFalse(queue.cancel("1"))
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun `given a large file waiting at the head when cancelled then the small files behind it start`() {
        queue.enqueue(downloads(small("1")))
        queue.enqueue(downloads(large("big"), small("2")))

        queue.cancel("big")

        assertEquals(listOf("1", "2"), started)
    }

    @Test
    fun `given files waiting when every pending one is cancelled then each is reported cancelled`() {
        queue.enqueue(downloads(small("1"), small("2"), small("3"), small("4")))

        queue.cancelPending()
        settle("1")

        assertEquals(listOf("3", "4"), cancelled)
        assertEquals(listOf("1", "2"), started)
        assertTrue(pathCache.offlineDownloadStates.value.queued.isEmpty())
    }

    @Test
    fun `given the queue offline when files are enqueued then none starts until back online`() {
        queue.setOnline(false)

        queue.enqueue(downloads(small("1"), small("2")))

        assertTrue(started.isEmpty())
        assertEquals(setOf("1", "2"), pathCache.offlineDownloadStates.value.queued)

        queue.setOnline(true)

        assertEquals(listOf("1", "2"), started)
    }

    @Test
    fun `given a download paused by the lost connection when back online then it starts again before the others`() {
        val first = small("1")
        queue.enqueue(listOf(first) + downloads(small("2"), small("3")))

        queue.setOnline(false)
        queue.requeuePaused(first)
        settle("1")

        assertEquals(listOf("1", "2"), started)
        assertTrue(queue.contains("1"))
        assertTrue("1" in pathCache.offlineDownloadStates.value.paused)

        queue.setOnline(true)

        assertEquals(listOf("1", "2", "1"), started)
    }

    private fun settle(uuid: String) = settleCallbacks.getValue(uuid)()

    private fun downloads(vararg downloads: QueuedOfflineDownload) = downloads.toList()

    private fun small(uuid: String) = queuedDownload(uuid, size = 10)

    private fun large(uuid: String) = queuedDownload(uuid, size = LARGE_FILE_THRESHOLD + 1)

    private fun queuedDownload(uuid: String, size: Long) = QueuedOfflineDownload(
        file = file(uuid, size),
        onSaved = {},
        onFailed = {},
        onCancelled = { cancelled += uuid },
    )

    private fun file(uuid: String, size: Long): CellNodeUi.File = Node.File(
        uuid = uuid,
        conversationId = "conversationId",
        versionId = "versionId",
        name = "$uuid.pdf",
        mimeType = "application/pdf",
        remotePath = "conversationId/$uuid.pdf",
        localPath = null,
        contentUrl = null,
        size = size,
        modifiedTime = 0L,
    ).toUiModel()

    private companion object {
        const val MAX_CONCURRENT = 2
        const val LARGE_FILE_THRESHOLD = 1000L
    }
}
