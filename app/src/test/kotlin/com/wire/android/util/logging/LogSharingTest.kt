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

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class LogSharingTest {

    @get:Rule
    val tempDir: TemporaryFolder = TemporaryFolder()

    @Test
    fun `given log files when creating compressed logs archive then all logs are written into one zip`() {
        val logsDirectory = tempDir.newFolder("logs")
        File(logsDirectory, "wire_logs.txt").writeText("active logs")
        File(logsDirectory, "wire_logs.1.gz").writeText("rotated logs")
        File(logsDirectory, "wire_logs.2.gz.tmp").writeText("in-progress rotation")
        File(logsDirectory, "nested").mkdir()
        val archive = File(tempDir.root, "wire_logs.zip")

        val result = createCompressedLogsArchive(logsDirectory, archive)

        assertEquals(archive, result)
        assertTrue(archive.exists())
        ZipFile(archive).use { zipFile ->
            val entryNames = zipFile.entries().asSequence().map { it.name }.toList()
            assertEquals(listOf("wire_logs.1.gz", "wire_logs.txt"), entryNames)
            assertEquals("active logs", zipFile.readEntryText("wire_logs.txt"))
            assertEquals("rotated logs", zipFile.readEntryText("wire_logs.1.gz"))
        }
    }

    @Test
    fun `given existing compressed logs archive when creating new archive then stale content is removed`() {
        val logsDirectory = tempDir.newFolder("logs")
        File(logsDirectory, "wire_logs.txt").writeText("fresh logs")
        val archive = File(tempDir.root, "wire_logs.zip").apply {
            writeText("stale archive")
        }

        createCompressedLogsArchive(logsDirectory, archive)

        ZipFile(archive).use { zipFile ->
            val entryNames = zipFile.entries().asSequence().map { it.name }.toList()
            assertEquals(listOf("wire_logs.txt"), entryNames)
            assertEquals("fresh logs", zipFile.readEntryText("wire_logs.txt"))
        }
    }

    @Test
    fun `given expired compressed logs archives when deleting stale archives then recent archives are kept`() {
        val cacheDirectory = tempDir.newFolder("cache")
        val archiveToKeep = File(cacheDirectory, "wire-logs-2026-05-22_12-34-56-current.zip").apply {
            writeText("current")
        }
        val expiredArchive = File(cacheDirectory, "wire-logs-2026-05-21_12-34-55-stale.zip").apply {
            writeText("expired")
            setLastModified(EXPIRED_ARCHIVE_LAST_MODIFIED)
        }
        val recentArchive = File(cacheDirectory, "wire-logs-2026-05-22_12-34-55-recent.zip").apply {
            writeText("recent")
            setLastModified(RECENT_ARCHIVE_LAST_MODIFIED)
        }
        val unrelatedZip = File(cacheDirectory, "backup.zip").apply {
            writeText("unrelated")
        }

        deleteStaleCompressedLogsArchives(
            directory = cacheDirectory,
            keepFile = archiveToKeep,
            currentTimeMillis = CURRENT_TIME_MILLIS,
            retentionMillis = ARCHIVE_RETENTION_MILLIS
        )

        assertTrue(archiveToKeep.exists())
        assertTrue(recentArchive.exists())
        assertTrue(unrelatedZip.exists())
        assertTrue(!expiredArchive.exists())
    }

    @Test
    fun `given instant when creating compressed logs archive file name then it includes local date time and random suffix`() {
        val instant = Instant.parse("2026-05-22T12:34:56Z")

        val result = compressedLogsArchiveFileName(
            instant = instant,
            timeZone = TimeZone.UTC,
            randomSuffix = "abc123ef"
        )

        assertEquals("wire-logs-2026-05-22_12-34-56-abc123ef.zip", result)
    }

    private fun ZipFile.readEntryText(name: String): String =
        getInputStream(getEntry(name)).bufferedReader().use { it.readText() }

    private companion object {
        const val CURRENT_TIME_MILLIS = 1_000L
        const val ARCHIVE_RETENTION_MILLIS = 100L
        const val EXPIRED_ARCHIVE_LAST_MODIFIED = CURRENT_TIME_MILLIS - ARCHIVE_RETENTION_MILLIS - 1L
        const val RECENT_ARCHIVE_LAST_MODIFIED = CURRENT_TIME_MILLIS - ARCHIVE_RETENTION_MILLIS + 1L
    }
}
