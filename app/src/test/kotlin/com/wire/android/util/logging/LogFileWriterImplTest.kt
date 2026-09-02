/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.util.logging

import co.touchlab.kermit.Severity
import com.wire.android.AppLogger
import com.wire.android.appLogger
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.common.logger.kaliumLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipFile

class LogFileWriterImplTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @After
    fun resetLoggers() {
        AppLogger.init(KaliumLogger.Config.DISABLED)
        CoreLogger.init(KaliumLogger.Config.DISABLED)
    }

    @Test
    fun `given enabled writer when application and Kalium log then both are persisted`() = runTest {
        val writer = newWriter()
        writer.start()
        val config = KaliumLogger.Config(KaliumLogLevel.INFO, listOf(writer.logWriter))
        AppLogger.init(config)
        CoreLogger.init(config)

        appLogger.i("application diagnostic")
        kaliumLogger.i("Kalium diagnostic")
        writer.forceFlush()

        val logText = writer.activeLoggingFile.readText()
        assertTrue(logText.contains("application diagnostic"))
        assertTrue(logText.contains("Kalium diagnostic"))
    }

    @Test
    fun `given stopped writer when logging then diagnostics are not persisted`() = runTest {
        val writer = newWriter()
        writer.start()
        writer.stop()

        writer.logWriter.log(Severity.Info, "disabled diagnostic", "test", null)
        writer.forceFlush()

        assertEquals("", writer.activeLoggingFile.readText())
    }

    @Test
    fun `given small rolling limit when logging then retained files are bounded`() = runTest {
        val writer = newWriter(rollOnSizeBytes = 1, maxLogFiles = 3)
        writer.start()

        repeat(5) { index ->
            writer.logWriter.log(Severity.Info, "rolling diagnostic $index", "test", null)
            writer.forceFlush()
        }

        val logFiles = writer.activeLoggingFile.parentFile
            ?.listFiles { file -> file.name.matches(Regex("wire_logs(-[0-9]+)?\\.log")) }
            .orEmpty()

        assertTrue(logFiles.size <= 3)
        assertTrue(logFiles.any { it.name == "wire_logs-1.log" })
    }

    @Test
    fun `given deleted logs when logging resumes then active file is cleared and accepts new entries`() = runTest {
        val writer = newWriter()
        writer.start()
        writer.logWriter.log(Severity.Info, "before delete", "test", null)
        writer.forceFlush()

        writer.deleteAllLogFiles()

        assertTrue(writer.activeLoggingFile.exists())
        assertEquals("", writer.activeLoggingFile.readText())
        assertFalse(writer.activeLoggingFile.parentFile?.listFiles().orEmpty().any { it.name == "wire_logs-1.log" })

        writer.logWriter.log(Severity.Info, "after delete", "test", null)
        writer.forceFlush()

        assertTrue(writer.activeLoggingFile.readText().contains("after delete"))
    }

    @Test
    fun `given flushed logs when archiving then the active diagnostic file is included`() = runTest {
        val writer = newWriter()
        writer.start()
        writer.logWriter.log(Severity.Info, "shareable diagnostic", "test", null)
        writer.forceFlush()
        val archive = File(tempDir.root, "logs.zip")

        createCompressedLogsArchive(writer.activeLoggingFile.parentFile!!, archive)

        ZipFile(archive).use { zip ->
            assertTrue(zip.getEntry(writer.activeLoggingFile.name) != null)
            val entry = zip.getEntry(writer.activeLoggingFile.name)
            val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            assertTrue(text.contains("shareable diagnostic"))
        }
    }

    @Test
    fun `given legacy log when starting then ZIP export includes one gzip snapshot`() = runTest {
        val logsDirectory = tempDir.newFolder("legacy-logs")
        val legacyActive = File(logsDirectory, "wire_logs.txt").apply { writeText("legacy diagnostic") }
        File(logsDirectory, "wire_2026-08-27_15-09-01.gz").writeText("old archive")
        val writer = writerFor(logsDirectory)

        writer.start()
        writer.logWriter.log(Severity.Info, "current diagnostic", "test", null)
        writer.forceFlush()
        val archive = File(tempDir.root, "migrated-logs.zip")
        createCompressedLogsArchive(logsDirectory, archive)

        ZipFile(archive).use { zip ->
            assertEquals("legacy diagnostic", zip.getInputStream(zip.getEntry("wire_legacy_active.gz"))
                .use { GZIPInputStream(it).bufferedReader().readText() })
            assertTrue(zip.getInputStream(zip.getEntry("wire_logs.log")).bufferedReader().readText().contains("current diagnostic"))
            assertEquals(null, zip.getEntry("wire_logs.txt"))
            assertEquals(null, zip.getEntry("wire_2026-08-27_15-09-01.gz"))
        }
        assertFalse(legacyActive.exists())
    }

    @Test
    fun `given finalized snapshot without source when starting then remaining legacy files are cleaned`() = runTest {
        val logsDirectory = tempDir.newFolder("interrupted-logs")
        File(logsDirectory, "wire_legacy_active.gz").writeGzip("legacy diagnostic")
        val legacyArchive = File(logsDirectory, "wire_2026-08-27_15-09-01.gz").apply { writeText("old archive") }

        writerFor(logsDirectory).start()

        assertFalse(legacyArchive.exists())
        assertEquals("legacy diagnostic", File(logsDirectory, "wire_legacy_active.gz").gzipText())
    }

    @Test
    fun `given snapshot failure when starting then legacy files remain and direct logging continues`() = runTest {
        val logsDirectory = tempDir.newFolder("failed-migration-logs")
        val legacyActive = File(logsDirectory, "wire_logs.txt").apply { writeText("legacy diagnostic") }
        val legacyArchive = File(logsDirectory, "wire_2026-08-27_15-09-01.gz").apply { writeText("old archive") }
        File(logsDirectory, "wire_legacy_active.gz").apply {
            mkdir()
            File(this, "blocker").writeText("cannot delete")
        }
        val writer = writerFor(logsDirectory)

        writer.start()
        writer.logWriter.log(Severity.Info, "current diagnostic", "test", null)
        writer.forceFlush()

        assertTrue(legacyActive.exists())
        assertTrue(legacyArchive.exists())
        assertTrue(writer.activeLoggingFile.readText().contains("current diagnostic"))
    }

    @Test
    fun `given legacy and direct logs when deleting then recognized files are removed and unrelated files remain`() = runTest {
        val writer = newWriter()
        writer.start()
        File(writer.activeLoggingFile.parentFile, "wire_logs.txt").writeText("legacy active")
        File(writer.activeLoggingFile.parentFile, "wire_2026-08-27_15-09-01.gz").writeText("legacy archive")
        File(writer.activeLoggingFile.parentFile, "wire_legacy_active.gz").writeGzip("legacy snapshot")
        val unrelatedFile = File(writer.activeLoggingFile.parentFile, "unrelated.gz").apply { writeText("keep") }

        writer.deleteAllLogFiles()

        assertFalse(File(writer.activeLoggingFile.parentFile, "wire_logs.txt").exists())
        assertFalse(File(writer.activeLoggingFile.parentFile, "wire_2026-08-27_15-09-01.gz").exists())
        assertFalse(File(writer.activeLoggingFile.parentFile, "wire_legacy_active.gz").exists())
        assertTrue(unrelatedFile.exists())
    }

    private fun newWriter(
        rollOnSizeBytes: Long = 25 * 1024 * 1024L,
        maxLogFiles: Int = 10,
    ) = LogFileWriterImpl(
        logsDirectory = tempDir.newFolder("logs"),
        config = LogFileWriterConfig(
            rollOnSizeBytes = rollOnSizeBytes,
            maxLogFiles = maxLogFiles,
        )
    )

    private fun writerFor(logsDirectory: File) = LogFileWriterImpl(
        logsDirectory = logsDirectory,
        config = LogFileWriterConfig(
            rollOnSizeBytes = 25 * 1024 * 1024L,
            maxLogFiles = 10,
        )
    )

    private fun File.writeGzip(contents: String) {
        GZIPOutputStream(outputStream()).bufferedWriter().use { it.write(contents) }
    }

    private fun File.gzipText(): String = GZIPInputStream(inputStream()).bufferedReader().use { it.readText() }
}
