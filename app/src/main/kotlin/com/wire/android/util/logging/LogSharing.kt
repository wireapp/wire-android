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

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.util.EmailComposer
import com.wire.android.util.externalShareChooserIntent
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.sha256
import com.wire.android.util.shareableFileProviderUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val LOGS_ARCHIVE_MIME_TYPE = "application/zip"
private const val LOGS_ARCHIVE_CACHE_DIRECTORY = "shared-logs"
private const val LOGS_ARCHIVE_FILE_PREFIX = "wire-logs-"
private const val LOGS_ARCHIVE_FILE_EXTENSION = ".zip"
private const val LOGS_ARCHIVE_RANDOM_SUFFIX_LENGTH = 8
private const val LOGS_ARCHIVE_RETENTION_MILLIS = 24L * 60L * 60L * 1000L

class LogShareLauncher(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val archiveCreator: CompressedLogsArchiveCreator = CompressedLogsArchiveCreator(context.cacheDir),
    private val onFailure: (Throwable) -> Unit = {}
) {
    fun shareLogs(
        logsDirectory: File,
        flushLogs: suspend () -> Unit = {}
    ) {
        share(
            logsDirectory = logsDirectory,
            flushLogs = flushLogs,
            shareArchive = { archive ->
                context.startActivity(context.externalShareChooserIntent(context.logsSharingIntent(archive)))
            }
        )
    }

    fun shareLogsViaWire(
        logsDirectory: File,
        onShareUri: (Uri) -> Unit,
        flushLogs: suspend () -> Unit = {}
    ) {
        share(
            logsDirectory = logsDirectory,
            flushLogs = flushLogs,
            shareArchive = { archive -> onShareUri(context.logsSharingUri(archive)) }
        )
    }

    fun shareBugReport(
        flushLogs: suspend () -> Unit = {}
    ) {
        share(
            logsDirectory = LogFileWriter.logsDirectory(context),
            flushLogs = flushLogs,
            shareArchive = { archive -> context.startActivity(context.bugReportLogsSharingIntent(archive)) }
        )
    }

    private fun share(
        logsDirectory: File,
        flushLogs: suspend () -> Unit,
        shareArchive: (File) -> Unit
    ) {
        coroutineScope.launch {
            runCatching {
                flushLogs()
                val archive = archiveCreator.create(logsDirectory)
                shareArchive(archive)
            }.onFailure { error ->
                appLogger.e("Failed to prepare logs for sharing", error)
                onFailure(error)
            }
        }
    }
}

class CompressedLogsArchiveCreator(
    private val cacheDirectory: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun create(logsDirectory: File): File = withContext(dispatcher) {
        val outputDirectory = File(cacheDirectory, LOGS_ARCHIVE_CACHE_DIRECTORY)
        val outputFile = File(outputDirectory, compressedLogsArchiveFileName())
        deleteStaleCompressedLogsArchives(
            directory = outputDirectory,
            keepFile = outputFile,
            currentTimeMillis = currentTimeMillis()
        )
        createCompressedLogsArchive(logsDirectory, outputFile)
    }
}

fun Context.logsSharingUri(archiveFile: File): Uri = shareableFileProviderUri(archiveFile)

fun Context.logsSharingIntent(archiveFile: File): Intent {
    val archiveUri = logsSharingUri(archiveFile)
    return Intent(Intent.ACTION_SEND).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = LOGS_ARCHIVE_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, archiveUri)
        clipData = ClipData.newUri(contentResolver, archiveFile.name, archiveUri)
    }
}

fun Context.bugReportLogsSharingIntent(archiveFile: File): Intent {
    val intent = logsSharingIntent(archiveFile).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.send_bug_report_email)))
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.send_bug_report_subject))
        putExtra(
            Intent.EXTRA_TEXT,
            EmailComposer.reportBugEmailTemplate(
                getDeviceIdString()?.sha256(),
                getGitBuildId()
            )
        )
        selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
    }
    return externalShareChooserIntent(intent, getString(R.string.send_feedback_choose_email))
}

internal fun deleteStaleCompressedLogsArchives(
    directory: File?,
    keepFile: File,
    currentTimeMillis: Long = System.currentTimeMillis(),
    retentionMillis: Long = LOGS_ARCHIVE_RETENTION_MILLIS
) {
    directory?.listFiles()
        ?.filter { file ->
            file.isFile &&
                file != keepFile &&
                file.name.startsWith(LOGS_ARCHIVE_FILE_PREFIX) &&
                file.name.endsWith(LOGS_ARCHIVE_FILE_EXTENSION) &&
                file.lastModified() < currentTimeMillis - retentionMillis
        }
        ?.forEach(File::delete)
}

fun createCompressedLogsArchive(logsDirectory: File, outputFile: File): File {
    outputFile.parentFile?.mkdirs()
    if (outputFile.exists()) {
        outputFile.delete()
    }
    ZipOutputStream(BufferedOutputStream(outputFile.outputStream())).use { zipStream ->
        logsDirectory.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".tmp") }
            ?.sortedBy(File::getName)
            ?.forEach { file ->
                zipStream.putNextEntry(ZipEntry(file.name))
                BufferedInputStream(file.inputStream()).use { input ->
                    input.copyTo(zipStream)
                }
                zipStream.closeEntry()
            }
    }
    return outputFile
}

fun compressedLogsArchiveFileName(
    instant: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    randomSuffix: String = UUID.randomUUID().toString().take(LOGS_ARCHIVE_RANDOM_SUFFIX_LENGTH)
): String {
    val dateTime = instant.toLocalDateTime(timeZone)
    return "$LOGS_ARCHIVE_FILE_PREFIX${dateTime.year}-${dateTime.monthNumber.padded()}-${dateTime.dayOfMonth.padded()}" +
        "_${dateTime.hour.padded()}-${dateTime.minute.padded()}-${dateTime.second.padded()}-$randomSuffix.zip"
}

private fun Int.padded(): String = toString().padStart(2, '0')
