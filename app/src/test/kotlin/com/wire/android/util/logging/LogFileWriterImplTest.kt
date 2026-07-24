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
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LogFileWriterImplTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun givenFileWriterIsStopped_whenKermitEmitsLog_thenActiveFileIsNotCreated() {
        val writer = LogFileWriterImpl(logsDirectory = logsDirectory())

        writer.logWriter.log(Severity.Info, "ignored", "TestTag", null)

        assertFalse(writer.activeLoggingFile.exists())
    }

    @Test
    fun givenFileWriterIsNotStarted_whenDeletingAllLogs_thenActiveAndCompressedLogsAreDeleted() = runBlocking {
        val writer = LogFileWriterImpl(logsDirectory = logsDirectory())
        val compressedFile = logsDirectory().resolve("old-log.gz")
        logsDirectory().mkdirs()
        writer.activeLoggingFile.writeText("active")
        compressedFile.writeText("compressed")

        writer.deleteAllLogFiles()

        assertEquals("", writer.activeLoggingFile.readText())
        assertFalse(compressedFile.exists())
    }

    private fun logsDirectory() = tempDir.resolve("logs")
}
