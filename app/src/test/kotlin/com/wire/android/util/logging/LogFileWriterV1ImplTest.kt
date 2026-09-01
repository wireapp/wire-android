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
}
