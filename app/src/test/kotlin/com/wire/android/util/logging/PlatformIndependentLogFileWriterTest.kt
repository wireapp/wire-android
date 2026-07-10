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
package com.wire.android.util.logging

import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class PlatformIndependentLogFileWriterTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun givenFileWriterIsStarted_whenKermitEmitsLog_thenLogIsWrittenToActiveFile() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(flushIntervalMs = ONE_MINUTE_MS)
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "hello from kermit", "TestTag", null)
        writer.forceFlush()

        val logText = writer.activeLoggingFile.readText()
        assertTrue(logText.contains(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} Info: \(TestTag\)""")))
        assertTrue(logText.contains("TestTag"))
        assertTrue(logText.contains("hello from kermit"))
        writer.stop()
    }

    @Test
    fun givenFileWriterIsStarted_whenKermitEmitsLog_thenLogIsNotSynchronouslyFlushed() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(
                flushIntervalMs = ONE_MINUTE_MS,
                maxBufferSize = TWO_LOG_LINES
            )
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "buffered only", "TestTag", null)
        waitForWorker()

        assertFalse(writer.activeLoggingFile.readText().contains("buffered only"))
        writer.forceFlush()
        assertTrue(writer.activeLoggingFile.readText().contains("buffered only"))
        writer.stop()
    }

    @Test
    fun givenBufferReachesMaxSize_whenKermitEmitsLogs_thenWorkerFlushesBuffer() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(
                flushIntervalMs = ONE_MINUTE_MS,
                maxBufferSize = TWO_LOG_LINES
            )
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "first", "TestTag", null)
        writer.logWriter.log(Severity.Info, "second", "TestTag", null)

        eventually {
            val logText = writer.activeLoggingFile.readText()
            logText.contains("first") && logText.contains("second")
        }
        writer.stop()
    }

    @Test
    fun givenFileWriterIsStopped_whenKermitEmitsLog_thenActiveFileIsNotCreated() {
        val writer = PlatformIndependentLogFileWriter(logsDirectory = logsDirectory())

        writer.logWriter.log(Severity.Info, "ignored", "TestTag", null)

        assertFalse(writer.activeLoggingFile.exists())
    }

    @Test
    fun givenFileWriterIsStopped_whenStartedAgain_thenPreviousSessionLogsAreCleared() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(flushIntervalMs = ONE_MINUTE_MS)
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "previous session", "TestTag", null)
        writer.forceFlush()
        assertTrue(writer.activeLoggingFile.readText().contains("previous session"))

        writer.stop()
        assertEquals("", writer.activeLoggingFile.readText())

        writer.start()
        writer.logWriter.log(Severity.Info, "next session", "TestTag", null)
        writer.forceFlush()

        val logText = writer.activeLoggingFile.readText()
        assertFalse(logText.contains("previous session"))
        assertTrue(logText.contains("next session"))
        writer.stop()
    }

    @Test
    fun givenActiveFileExceedsMaxSize_whenKermitEmitsLog_thenWorkerRotatesActiveFile() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(
                flushIntervalMs = ONE_MINUTE_MS,
                maxBufferSize = ONE_LOG_LINE,
                maxFileSize = SMALL_FILE_SIZE_BYTES
            )
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "large-message-${"x".repeat(LARGE_MESSAGE_SIZE)}", "TestTag", null)
        writer.forceFlush()

        eventually { compressedLogFiles().size == 1 }
        val compressedFile = compressedLogFiles().single()
        val compressedText = GZIPInputStream(compressedFile.inputStream()).bufferedReader().use { it.readText() }
        assertTrue(compressedText.contains("large-message"))
        assertFalse(writer.activeLoggingFile.readText().contains("large-message"))
        writer.stop()
    }

    @Test
    fun givenTwoRotationsHaveTheSameTimestamp_whenLogsAreCompressed_thenDistinctArchivesArePublished() = runBlocking {
        val writer = testWriter(rotationTimestamp = FIXED_ROTATION_TIMESTAMP)

        writer.start()
        writer.logWriter.log(Severity.Info, "first-${"x".repeat(LARGE_MESSAGE_SIZE)}", "TestTag", null)
        eventually { compressedLogFiles().size == 1 }
        writer.logWriter.log(Severity.Info, "second-${"x".repeat(LARGE_MESSAGE_SIZE)}", "TestTag", null)
        eventually { compressedLogFiles().size == 2 }

        assertEquals(
            setOf("wire_${FIXED_ROTATION_TIMESTAMP}_1.gz", "wire_${FIXED_ROTATION_TIMESTAMP}_2.gz"),
            compressedLogFiles().map(File::getName).toSet()
        )
        writer.stop()
    }

    @Test
    fun givenCompressionFails_whenActiveFileRotates_thenSourceIsPreservedAndPartialOutputIsDeleted() = runBlocking {
        val writer = testWriter(
            rotationTimestamp = FIXED_ROTATION_TIMESTAMP,
            fileCompressor = { _, targetFile ->
                targetFile.writeText("incomplete gzip")
                throw IOException("compression failed")
            }
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "preserve-${"x".repeat(LARGE_MESSAGE_SIZE)}", "TestTag", null)

        eventually {
            temporaryLogFiles().singleOrNull()?.readText()?.contains("preserve-") == true &&
                partialLogFiles().isEmpty()
        }
        assertTrue(compressedLogFiles().isEmpty())
        writer.stop()
    }

    @Test
    fun givenDeleteAllLogsWhileCompressionIsInFlight_whenCompressionFinishes_thenNoRotationFilesRemain() = runBlocking {
        val compressionStarted = CountDownLatch(1)
        val releaseCompression = CountDownLatch(1)
        val compressionFinished = CountDownLatch(1)
        val writer = testWriter(
            rotationTimestamp = FIXED_ROTATION_TIMESTAMP,
            fileCompressor = { _, targetFile ->
                compressionStarted.countDown()
                try {
                    check(releaseCompression.await(COMPRESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    targetFile.writeText("compressed")
                } finally {
                    compressionFinished.countDown()
                }
            }
        )

        writer.start()
        writer.logWriter.log(Severity.Info, "delete-${"x".repeat(LARGE_MESSAGE_SIZE)}", "TestTag", null)
        eventually { compressionStarted.count == 0L }

        writer.deleteAllLogFiles()
        releaseCompression.countDown()

        eventually { compressionFinished.count == 0L }
        eventually {
            compressedLogFiles().isEmpty() && temporaryLogFiles().isEmpty() && partialLogFiles().isEmpty()
        }
        writer.stop()
    }

    @Test
    fun givenOrphanCompressionInitiallyFails_whenLaterStartupRetries_thenArchiveIsPublishedAndOrphanIsRemoved() = runBlocking {
        logsDirectory().mkdirs()
        val orphan = logsDirectory().resolve("wire_orphan.gz.tmp").apply { writeText("diagnostic logs") }
        val failingWriter = testWriter(
            rotationTimestamp = FIXED_ROTATION_TIMESTAMP,
            fileCompressor = { _, targetFile ->
                targetFile.writeText("incomplete gzip")
                throw IOException("compression failed")
            }
        )

        failingWriter.start()

        assertTrue(orphan.exists())
        assertEquals("diagnostic logs", orphan.readText())
        assertFalse(logsDirectory().resolve("wire_orphan.gz").exists())
        assertTrue(partialLogFiles().isEmpty())
        failingWriter.stop()

        val retryingWriter = testWriter(rotationTimestamp = FIXED_ROTATION_TIMESTAMP)
        retryingWriter.start()

        val recoveredArchive = logsDirectory().resolve("wire_orphan.gz")
        assertTrue(recoveredArchive.exists())
        assertFalse(orphan.exists())
        assertTrue(partialLogFiles().isEmpty())
        val recoveredText = GZIPInputStream(recoveredArchive.inputStream()).bufferedReader().use { it.readText() }
        assertEquals("diagnostic logs", recoveredText)
        retryingWriter.stop()
    }

    @Test
    fun givenFileWriterIsStarted_whenDeletingAllLogs_thenActiveAndCompressedLogsAreDeleted() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(
            logsDirectory = logsDirectory(),
            config = PlatformIndependentLogFileWriterConfig.default().copy(flushIntervalMs = ONE_MINUTE_MS)
        )
        val compressedFile = logsDirectory().resolve("old-log.gz")

        writer.start()
        writer.logWriter.log(Severity.Info, "to be deleted", "TestTag", null)
        writer.forceFlush()
        compressedFile.writeText("compressed")

        writer.deleteAllLogFiles()

        assertEquals("", writer.activeLoggingFile.readText())
        assertFalse(compressedFile.exists())
        writer.stop()
    }

    @Test
    fun givenFileWriterIsNotStarted_whenDeletingAllLogs_thenActiveAndCompressedLogsAreDeleted() = runBlocking {
        val writer = PlatformIndependentLogFileWriter(logsDirectory = logsDirectory())
        val compressedFile = logsDirectory().resolve("old-log.gz")
        logsDirectory().mkdirs()
        writer.activeLoggingFile.writeText("active")
        compressedFile.writeText("compressed")

        writer.deleteAllLogFiles()

        assertEquals("", writer.activeLoggingFile.readText())
        assertFalse(compressedFile.exists())
    }

    @Test
    fun givenTimestampFormatter_whenFormattingMillis_thenMillisecondsArePadded() {
        val formatter = LogLineTimestampFormatter()

        assertTrue(formatter.format(1_005L).endsWith(".005"))
        assertTrue(formatter.format(1_040L).endsWith(".040"))
        assertTrue(formatter.format(1_400L).endsWith(".400"))
    }

    private fun logsDirectory() = tempDir.resolve("logs")

    private fun compressedLogFiles() = logsDirectory().listFiles().orEmpty()
        .filter { it.extension == "gz" }

    private fun temporaryLogFiles() = logsDirectory().listFiles().orEmpty()
        .filter { it.name.endsWith(".tmp") }

    private fun partialLogFiles() = logsDirectory().listFiles().orEmpty()
        .filter { it.name.endsWith(".partial") }

    private fun testWriter(
        rotationTimestamp: String,
        fileCompressor: (File, File) -> Unit = ::compressToGzip
    ) = PlatformIndependentLogFileWriter(
        logsDirectory = logsDirectory(),
        config = PlatformIndependentLogFileWriterConfig.default().copy(
            flushIntervalMs = ONE_MINUTE_MS,
            maxBufferSize = ONE_LOG_LINE,
            maxFileSize = SMALL_FILE_SIZE_BYTES
        ),
        fileCompressor = fileCompressor,
        rotationTimestampProvider = { rotationTimestamp }
    )

    private fun compressToGzip(sourceFile: File, targetFile: File) {
        GZIPOutputStream(targetFile.outputStream()).use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output) }
        }
    }

    private suspend fun eventually(assertion: () -> Boolean) {
        repeat(EVENTUALLY_RETRIES) {
            if (assertion()) return
            waitForWorker(EVENTUALLY_DELAY_MS)
        }
        assertTrue(assertion())
    }

    private suspend fun waitForWorker(delayMillis: Long = WORKER_SETTLE_MS) {
        withContext(Dispatchers.IO) {
            delay(delayMillis)
        }
    }

    private companion object {
        const val ONE_MINUTE_MS = 60_000L
        const val ONE_LOG_LINE = 1
        const val TWO_LOG_LINES = 2
        const val SMALL_FILE_SIZE_BYTES = 64L
        const val LARGE_MESSAGE_SIZE = 64
        const val WORKER_SETTLE_MS = 100L
        const val EVENTUALLY_RETRIES = 20
        const val EVENTUALLY_DELAY_MS = 50L
        const val FIXED_ROTATION_TIMESTAMP = "2026-01-02_03-04-05"
        const val COMPRESSION_TIMEOUT_SECONDS = 5L
    }
}
