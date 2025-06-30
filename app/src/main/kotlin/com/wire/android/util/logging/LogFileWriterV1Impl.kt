/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import com.wire.android.appLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

@Suppress("TooGenericExceptionCaught")
class LogFileWriterV1Impl(private val logsDirectory: File) : LogFileWriter {

    private val logFileTimeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    override val activeLoggingFile = File(logsDirectory, ACTIVE_LOGGING_FILE_NAME)

    private val fileWriterCoroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var writingJob: Job? = null

    /**
     * Initializes logging, waiting until the logger is actually initialized before returning.
     * ```kotlin
     * logFileWriter.start()
     * logger.i("something") // Is guaranteed to be recorded in the log file
     * ```
     */
    override suspend fun start() {
        appLogger.i("KaliumFileWritter.start called")
        val isWriting = writingJob?.isActive ?: false
        if (isWriting) {
            appLogger.d("KaliumFileWriter.init called but job was already active. Ignoring call")
            return
        }
        ensureLogDirectoryAndFileExistence()
        val waitInitializationJob = Job()

        writingJob = fileWriterCoroutineScope.launch {
            observeLogCatWritingToLoggingFile().catch {
                appLogger.e("Write to file failed :$it", it)
            }.onEach {
                waitInitializationJob.complete()
            }.filter {
                it > LOG_FILE_MAX_SIZE_THRESHOLD
            }.collect {
                ensureActive()
                compress()
                clearActiveLoggingFileContent()
                deleteOldCompressedFiles()
            }
        }
        appLogger.i("KaliumFileWritter.start: Starting log collection.")
        waitInitializationJob.join()
    }

    /**
     * Observes logcat text, writing to the [activeLoggingFile] as it reads.
     * @return A Flow that tells the current length, in bytes, of the log file.
     */
    private fun CoroutineScope.observeLogCatWritingToLoggingFile(): Flow<Long> = flow<Long> {
        Runtime.getRuntime().exec("logcat -c")
        val process = Runtime.getRuntime().exec("logcat")

        val reader = process.inputStream.bufferedReader()

        appLogger.i("Starting to write log files, grabbing from logcat")
        while (isActive) {
            val text = reader.readLine()
            if (!text.isNullOrBlank()) {
                val fileSize = writeLineToFile(text)
                emit(fileSize)
            }
        }
        reader.close()
        process.destroy()
    }.flowOn(Dispatchers.IO)

    /**
     * Stops processing logs and writing to files
     */
    override suspend fun stop() {
        appLogger.i("KaliumFileWritter.stop called; Stopping log collection.")
        writingJob?.cancel()
        clearActiveLoggingFileContent()
    }

    override suspend fun forceFlush() {
        /* no-op */
    }

    private fun clearActiveLoggingFileContent() {
        if (activeLoggingFile.exists()) {
            val writer = PrintWriter(activeLoggingFile)
            writer.print("")
            writer.close()
        }
    }

    /**
     * Writes the new [text] and other log entries in logcat to the [activeLoggingFile].
     * @return The length, in bytes, of the log file.
     */
    private fun writeLineToFile(text: String): Long {
        FileWriter(activeLoggingFile, true).use { fw ->
            fw.appendLine(text)
            fw.flush()
        }
        return activeLoggingFile.length()
    }

    private fun ensureLogDirectoryAndFileExistence() {
        if (!logsDirectory.exists() && !logsDirectory.mkdirs()) {
            appLogger.e("Unable to create logs directory")
        }

        if (!activeLoggingFile.exists() && !activeLoggingFile.createNewFile()) {
            appLogger.e("KaliumFileWriter: Failure to create new file for logging", IOException("Unable to load log file"))
        }
        if (!activeLoggingFile.canWrite()) {
            appLogger.e("KaliumFileWriter: Logging file is not writable", IOException("Log file not writable"))
        }
    }

    override fun deleteAllLogFiles() {
        clearActiveLoggingFileContent()
        logsDirectory.listFiles()?.filter {
            it.extension.lowercase(Locale.ROOT) == LOG_COMPRESSED_FILE_EXTENSION
        }?.forEach { it.delete() }
    }

    private fun getCompressedFilesList() = (logsDirectory.listFiles() ?: emptyArray()).filter { it != activeLoggingFile }

    private fun compressedFileName(): String {
        val currentDate = logFileTimeFormat.format(Date())
        return "${LOG_FILE_PREFIX}_$currentDate.$LOG_COMPRESSED_FILE_EXTENSION"
    }

    private fun deleteOldCompressedFiles() = getCompressedFilesList()
        .sortedBy { it.lastModified() }
        .dropLast(LOG_COMPRESSED_FILES_MAX_COUNT)
        .forEach {
            it.delete()
        }

    private fun compress(): Boolean {
        try {
            val compressed = File(logsDirectory, compressedFileName())
            val zippedOutputStream = GZIPOutputStream(compressed.outputStream())
            val inputStream = activeLoggingFile.inputStream()
            inputStream.copyTo(zippedOutputStream, BYTE_ARRAY_SIZE)
            clearActiveLoggingFileContent()
            inputStream.close()
            zippedOutputStream.close()
        } catch (e: IOException) {
            appLogger.d("$e")
            return false
        }

        return true
    }

    companion object {
        private const val LOG_FILE_PREFIX = "wire"
        private const val ACTIVE_LOGGING_FILE_NAME = "${LOG_FILE_PREFIX}_logs.txt"
        private const val LOG_FILE_MAX_SIZE_THRESHOLD = 25 * 1024 * 1024
        private const val BYTE_ARRAY_SIZE = 1024
        private const val LOG_COMPRESSED_FILES_MAX_COUNT = 10
        private const val LOG_COMPRESSED_FILE_EXTENSION = "gz"
    }
}
