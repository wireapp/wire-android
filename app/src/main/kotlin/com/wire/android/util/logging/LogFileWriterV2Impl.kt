/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPOutputStream

@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class LogFileWriterV2Impl(
    private val logsDirectory: File,
    private val config: LogFileWriterV2Config = LogFileWriterV2Config.default()
) : LogFileWriter {

    private val logFileTimeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    override val activeLoggingFile = File(logsDirectory, ACTIVE_LOGGING_FILE_NAME)

    private val fileWriterCoroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var writingJob: Job? = null
    private var flushJob: Job? = null

    // Buffering system
    private val logBuffer = mutableListOf<String>()
    private val bufferMutex = Mutex()
    private var lastFlushTime = 0L
    private var bufferedWriter: BufferedWriter? = null

    // Process management
    private var logcatProcess: Process? = null

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
                it > config.maxFileSize
            }.collect {
                ensureActive()
                // Flush buffer before compression
                bufferMutex.withLock {
                    flushBuffer()
                }
                launch { compressAsync() }
                clearActiveLoggingFileContent()
                deleteOldCompressedFiles()
            }
        }

        // Start periodic flush job
            flushJob = fileWriterCoroutineScope.launch {
                while (isActive) {
                    delay(config.flushIntervalMs)
                    try {
                        withTimeout(config.bufferLockTimeoutMs) {
                            bufferMutex.withLock {
                                if (logBuffer.isNotEmpty()) {
                                    flushBuffer()
                                    lastFlushTime = System.currentTimeMillis()
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        appLogger.w("Periodic flush timed out, buffer may be locked by another operation")
                    } catch (e: Exception) {
                        appLogger.e("Error during periodic flush", e)
                    }
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
        logcatProcess = Runtime.getRuntime().exec("logcat")

        val reader = logcatProcess!!.inputStream.bufferedReader()

        appLogger.i("Starting to write log files, grabbing from logcat")
        while (isActive) {
            val text = reader.readLine()
            if (!text.isNullOrBlank()) {
                val fileSize = writeLineToFile(text)
                emit(fileSize)
            }
        }
        reader.close()
        stopLogcatProcess()
    }.flowOn(Dispatchers.IO)

    private fun stopLogcatProcess() {
        logcatProcess?.let { process ->
            try {
                process.destroy()
                if (process.isAlive && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    process.destroyForcibly()
                }
            } catch (e: Exception) {
                appLogger.e("Error stopping logcat process", e)
            }
        }
        logcatProcess = null
    }

    /**
     * Stops processing logs and writing to files
     */
    override suspend fun stop() {
        appLogger.i("KaliumFileWritter.stop called; Stopping log collection.")
        try {
            // Stop logcat process first to prevent new logs
            stopLogcatProcess()
            // Cancel jobs with timeout to avoid hanging
            writingJob?.let { job ->
                try {
                    withTimeout(config.flushTimeoutMs) {
                        job.cancelAndJoin()
                    }
                } catch (e: TimeoutCancellationException) {
                    appLogger.w("Writing job cancellation timed out, forcing cancellation")
                    job.cancel()
                }
            }

            flushJob?.let { job ->
                try {
                    withTimeout(config.flushTimeoutMs) {
                        job.cancelAndJoin()
                    }
                } catch (e: TimeoutCancellationException) {
                    appLogger.w("Flush job cancellation timed out, forcing cancellation")
                    job.cancel()
                }
            }

            // Flush any remaining buffered content with timeout
            try {
                withTimeout(config.flushTimeoutMs) {
                    bufferMutex.withLock {
                        flushBuffer()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                appLogger.w("Final buffer flush timed out, some logs may be lost")
            } catch (e: Exception) {
                appLogger.e("Error during final buffer flush", e)
            }
        } finally {
            // Ensure resources are cleaned up regardless of exceptions
            closeResources()
            try {
                clearActiveLoggingFileContent()
            } catch (e: Exception) {
                appLogger.e("Error clearing active logging file content", e)
            }
        }
    }

    private fun closeResources() {
        try {
            bufferedWriter?.close()
        } catch (e: Exception) {
            appLogger.e("Error closing buffered writer", e)
        } finally {
            bufferedWriter = null
        }
    }

    /**
     * Manually flushes any buffered log entries to the file.
     * This is useful before sharing logs to ensure all recent entries are included.
     */
    override suspend fun forceFlush() {
        try {
            withTimeout(config.flushTimeoutMs) {
                bufferMutex.withLock {
                    flushBuffer()
                }
            }
        } catch (e: TimeoutCancellationException) {
            appLogger.w("Force flush operation timed out after ${config.flushTimeoutMs}ms")
            throw e
        } catch (e: Exception) {
            appLogger.e("Error during force flush", e)
            throw e
        }
    }

    private fun clearActiveLoggingFileContent() {
        if (activeLoggingFile.exists()) {
            PrintWriter(activeLoggingFile).use { writer ->
                writer.print("")
            }
        }
    }

    /**
     * Writes the new [text] and other log entries in logcat to the [activeLoggingFile].
     * @return The length, in bytes, of the log file.
     */
    private suspend fun writeLineToFile(text: String): Long = withContext(Dispatchers.IO) {
        try {
            withTimeout(config.bufferLockTimeoutMs) {
                bufferMutex.withLock {
                    logBuffer.add(text)

                    val currentTime = System.currentTimeMillis()
                    val shouldFlush = logBuffer.size >= config.maxBufferSize ||
                        ((currentTime - lastFlushTime) >= config.flushIntervalMs)

                    if (shouldFlush) {
                        flushBuffer()
                        lastFlushTime = currentTime
                    }

                    return@withLock activeLoggingFile.length()
                }
            }
        } catch (e: TimeoutCancellationException) {
            appLogger.w("Buffer write operation timed out, log line may be lost: $text")
            // Return current file length as fallback
            return@withContext activeLoggingFile.length()
        } catch (e: Exception) {
            appLogger.e("Error writing to log buffer", e)
            return@withContext activeLoggingFile.length()
        }
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

    private suspend fun compressAsync() = withContext(Dispatchers.IO) {
        try {
            val compressedFile = File(logsDirectory, compressedFileName())

            GZIPOutputStream(compressedFile.outputStream().buffered()).use { gzipOut ->
                activeLoggingFile.inputStream().buffered().use { input ->
                    input.copyTo(gzipOut, config.bufferSizeBytes)
                }
            }

            appLogger.i("Log file compressed: ${activeLoggingFile.name} -> ${compressedFile.name}")
        } catch (e: Exception) {
            appLogger.e("Failed to compress log file: ${activeLoggingFile.name}", e)
        }
    }

    private fun flushBuffer() {
        if (logBuffer.isEmpty()) return

        try {
            // Use BufferedWriter for efficient writing
            val writer = bufferedWriter ?: BufferedWriter(
                FileWriter(activeLoggingFile, true),
                config.bufferSizeBytes
            ).also { bufferedWriter = it }

            // Like here one, Write directly from buffer without copying to the .toList()
            logBuffer.forEach { line ->
                writer.appendLine(line)
            }
            writer.flush()

            // and here two, Clear only after successful write
            logBuffer.clear()

        } catch (e: IOException) {
            appLogger.e("Failed to flush log buffer", e)
        }
    }
        if (logBuffer.isEmpty()) return
        val linesToWrite = logBuffer.toList()
        logBuffer.clear()
        try {
            // Use BufferedWriter for efficient writing
            val writer = bufferedWriter ?: BufferedWriter(
                FileWriter(activeLoggingFile, true),
                config.bufferSizeBytes
            ).also { bufferedWriter = it }

            linesToWrite.forEach { line ->
                writer.appendLine(line)
            }
            writer.flush()
        } catch (e: IOException) {
            appLogger.e("Failed to flush log buffer", e)
            // Re-add failed lines back to buffer for retry
            logBuffer.addAll(0, linesToWrite)
        }
    }

    companion object {
        private const val LOG_FILE_PREFIX = "wire"
        private const val ACTIVE_LOGGING_FILE_NAME = "${LOG_FILE_PREFIX}_logs.txt"
        private const val LOG_COMPRESSED_FILES_MAX_COUNT = 10
        private const val LOG_COMPRESSED_FILE_EXTENSION = "gz"
    }
}
