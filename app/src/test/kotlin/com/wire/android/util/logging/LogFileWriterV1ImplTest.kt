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

import com.wire.android.AppLogger
import com.wire.android.appLogger
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.common.logger.kaliumLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class LogFileWriterV1ImplTest {

    @TempDir
    lateinit var temporaryDirectory: File

    @AfterEach
    fun resetLoggers() {
        AppLogger.init(KaliumLogger.Config.DISABLED)
        CoreLogger.init(KaliumLogger.Config.DISABLED)
    }

    @Test
    fun `given app and Kalium loggers when enabled then both events reach the active file`() = runTest {
        val writer = writer()
        writer.start()
        val config = KaliumLogger.Config(KaliumLogLevel.VERBOSE, listOf(writer.logWriter))
        AppLogger.init(config)
        CoreLogger.init(config)

        appLogger.i("app-log-event")
        kaliumLogger.i("kalium-log-event")
        writer.forceFlush()

        val contents = writer.activeLoggingFile.readText()
        assertTrue(contents.contains("app-log-event"))
        assertTrue(contents.contains("kalium-log-event"))
    }

    @Test
    fun `given stopped writer when events are logged then they are not persisted`() = runTest {
        val writer = writer()
        writer.start()
        writer.stop()

        writer.logWriter.log(co.touchlab.kermit.Severity.Debug, "disabled-event", "test", null)

        assertFalse(writer.activeLoggingFile.exists() && writer.activeLoggingFile.readText().contains("disabled-event"))
    }

    @Test
    fun `given small rolling limit when logging then retained files are bounded and shareable`() = runTest {
        val writer = writer(rollOnSizeBytes = 1, maxLogFiles = 3)
        writer.start()

        repeat(10) { writer.logWriter.log(co.touchlab.kermit.Severity.Info, "event-$it", "test", null) }
        writer.forceFlush()

        val files = temporaryDirectory.listFiles().orEmpty()
        assertTrue(files.any { it == writer.activeLoggingFile })
        assertTrue(files.all { it.name == "wire_logs.log" || it.name.matches(Regex("wire_logs-[0-9]+\\.log")) })
        assertTrue(files.size <= 3)
    }

    @Test
    fun `given existing logs when deleting then active file is cleared rolls are removed and logging resumes`() = runTest {
        val writer = writer()
        writer.start()
        repeat(4) { writer.logWriter.log(co.touchlab.kermit.Severity.Info, "before-delete-$it", "test", null) }
        writer.forceFlush()

        writer.deleteAllLogFiles()

        assertEquals("", writer.activeLoggingFile.readText())
        assertTrue(temporaryDirectory.listFiles().orEmpty().none { it.name.matches(Regex("wire_logs-[0-9]+\\.log")) })

        writer.logWriter.log(co.touchlab.kermit.Severity.Info, "after-delete", "test", null)
        writer.forceFlush()
        assertTrue(writer.activeLoggingFile.readText().contains("after-delete"))
    }

    @Test
    fun `given legacy log when starting then it is retained in one gzip snapshot`() = runTest {
        val legacyActive = File(temporaryDirectory, "wire_logs.txt").apply { writeText("legacy diagnostic") }
        File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").writeText("old archive")
        File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz.tmp").writeText("old temp")

        writer().start()

        val snapshot = File(temporaryDirectory, "wire_legacy_active.gz")
        assertEquals("legacy diagnostic", snapshot.gzipText())
        assertFalse(legacyActive.exists())
        assertTrue(temporaryDirectory.listFiles().orEmpty().none { it.name.startsWith("wire_2026-") })
        assertTrue(File(temporaryDirectory, "wire_logs.log").exists())
    }

    @Test
    fun `given finalized legacy snapshot and source when starting then source is removed without a duplicate`() = runTest {
        File(temporaryDirectory, "wire_logs.txt").writeText("legacy diagnostic")
        File(temporaryDirectory, "wire_legacy_active.gz").writeGzip("legacy diagnostic")
        File(temporaryDirectory, "wire_legacy_active.gz.tmp").writeText("stale temp")
        val legacyArchive = File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").apply { writeText("old archive") }

        writer().start()

        assertFalse(File(temporaryDirectory, "wire_logs.txt").exists())
        assertFalse(File(temporaryDirectory, "wire_legacy_active.gz.tmp").exists())
        assertFalse(legacyArchive.exists())
        assertEquals(1, temporaryDirectory.listFiles().orEmpty().count { it.name == "wire_legacy_active.gz" })
        assertEquals("legacy diagnostic", File(temporaryDirectory, "wire_legacy_active.gz").gzipText())
    }

    @Test
    fun `given finalized snapshot without source when starting then remaining legacy archives are cleaned`() = runTest {
        File(temporaryDirectory, "wire_legacy_active.gz").writeGzip("legacy diagnostic")
        val legacyArchive = File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").apply { writeText("old archive") }

        writer().start()

        assertFalse(legacyArchive.exists())
        assertEquals("legacy diagnostic", File(temporaryDirectory, "wire_legacy_active.gz").gzipText())
    }

    @Test
    fun `given snapshot failure when starting then legacy files are retained and direct logging starts`() = runTest {
        val legacyActive = File(temporaryDirectory, "wire_logs.txt").apply { writeText("legacy diagnostic") }
        val legacyArchive = File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").apply { writeText("old archive") }
        File(temporaryDirectory, "wire_legacy_active.gz").apply {
            mkdir()
            File(this, "blocker").writeText("cannot delete")
        }

        val writer = writer()
        writer.start()
        writer.logWriter.log(co.touchlab.kermit.Severity.Info, "direct after failed migration", "test", null)
        writer.forceFlush()

        assertTrue(legacyActive.exists())
        assertTrue(legacyArchive.exists())
        assertTrue(writer.activeLoggingFile.readText().contains("direct after failed migration"))
    }

    @Test
    fun `given legacy and direct logs when deleting then recognized logs are removed and unrelated files remain`() = runTest {
        val writer = writer()
        writer.start()
        writer.logWriter.log(co.touchlab.kermit.Severity.Info, "direct", "test", null)
        writer.forceFlush()
        File(temporaryDirectory, "wire_logs.txt").writeText("legacy active")
        File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").writeText("legacy archive")
        File(temporaryDirectory, "wire_legacy_active.gz").writeGzip("legacy snapshot")
        val unrelatedFile = File(temporaryDirectory, "unrelated.gz").apply { writeText("keep") }

        writer.deleteAllLogFiles()

        assertEquals("", writer.activeLoggingFile.readText())
        assertFalse(File(temporaryDirectory, "wire_logs.txt").exists())
        assertFalse(File(temporaryDirectory, "wire_2026-08-27_15-09-01.gz").exists())
        assertFalse(File(temporaryDirectory, "wire_legacy_active.gz").exists())
        assertTrue(unrelatedFile.exists())
    }

    private fun writer(
        rollOnSizeBytes: Long = 25L * 1024 * 1024,
        maxLogFiles: Int = 11,
    ) = LogFileWriterV1Impl(
        logsDirectory = temporaryDirectory,
        config = LogFileWriterV1Config(
            rollOnSizeBytes = rollOnSizeBytes,
            maxLogFiles = maxLogFiles,
            flushTimeoutMs = 5_000,
        ),
    )

    private fun File.writeGzip(contents: String) {
        GZIPOutputStream(outputStream()).bufferedWriter().use { it.write(contents) }
    }

    private fun File.gzipText(): String = GZIPInputStream(inputStream()).bufferedReader().use { it.readText() }
}
