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

import android.util.Log
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.wire.android.appLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
class PlatformIndependentLogFileWriter(
    private val logsDirectory: File,
    private val config: PlatformIndependentLogFileWriterConfig = PlatformIndependentLogFileWriterConfig.default()
) : LogFileWriter {

    private val timestampFormatter: ThreadLocal<LogLineTimestampFormatter> = ThreadLocal.withInitial {
        LogLineTimestampFormatter()
    }

    override val activeLoggingFile = File(logsDirectory, ACTIVE_LOGGING_FILE_NAME)

    private val fileWriterCoroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleMutex = Mutex()

    @Volatile
    private var writerJob: Job? = null

    @Volatile
    private var logCommandChannel: Channel<LogCommand>? = null

    private val logBuffer = mutableListOf<String>()
    private var bufferedWriter: BufferedWriter? = null

    private val isStarted = AtomicBoolean(false)

    override val logWriter: LogWriter = object : LogWriter() {
        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            writeLogEntry(severity, message, tag, throwable)
        }
    }

    /**
     * Initializes logging, waiting until the logger is actually initialized before returning.
     * ```kotlin
     * logFileWriter.start()
     * logger.i("something") // Is guaranteed to be recorded in the log file
     * ```
     */
    override suspend fun start() {
        lifecycleMutex.withLock {
            appLogger.i("KaliumFileWritter.start called")
            if (isStarted.get()) {
                appLogger.d("KaliumFileWriter.init called but job was already active. Ignoring call")
                return
            }

            try {
                withContext(Dispatchers.IO) {
                    ensureLogDirectoryAndFileExistence()
                }
                val channel = Channel<LogCommand>(LOG_COMMAND_CHANNEL_CAPACITY)
                val job = fileWriterCoroutineScope.launch {
                    processLogCommands(channel)
                }
                logCommandChannel = channel
                writerJob = job
                isStarted.set(true)
            } catch (e: Exception) {
                isStarted.set(false)
                logCommandChannel = null
                writerJob?.cancel()
                writerJob = null
                throw e
            }

            appLogger.i("KaliumFileWritter.start: Starting log collection.")
        }
    }

    /**
     * Stops processing logs and writing to files.
     */
    override suspend fun stop() {
        lifecycleMutex.withLock {
            appLogger.i("KaliumFileWritter.stop called; Stopping log collection.")
            if (!isStarted.getAndSet(false)) return

            val channel = logCommandChannel
            val job = writerJob
            try {
                if (channel != null) {
                    val completion = CompletableDeferred<Unit>()
                    withTimeout(config.flushTimeoutMs) {
                        channel.send(LogCommand.Stop(completion))
                        completion.await()
                        job?.cancelAndJoin()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                appLogger.w("Logger stop timed out, forcing cancellation")
                job?.cancel()
            } catch (e: Exception) {
                appLogger.e("Error stopping log file writer", e)
            } finally {
                logCommandChannel = null
                writerJob = null
            }
        }
    }

    private fun closeResources() {
        try {
            bufferedWriter?.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error closing buffered writer", e)
        } finally {
            bufferedWriter = null
        }
    }

    /**
     * Manually flushes any buffered log entries to the file.
     * This is useful before sharing logs to ensure all recent entries are included.
     */
    override suspend fun forceFlush() {
        val channel = logCommandChannel.takeIf { isStarted.get() }
        val completion = CompletableDeferred<Unit>()
        if (channel != null) {
            try {
                withTimeout(config.flushTimeoutMs) {
                    channel.send(LogCommand.Flush(completion))
                    completion.await()
                }
            } catch (e: TimeoutCancellationException) {
                appLogger.w("Force flush operation timed out after ${config.flushTimeoutMs}ms")
                throw e
            } catch (e: Exception) {
                appLogger.e("Error during force flush", e)
                throw e
            }
        }
    }

    private fun clearActiveLoggingFileContent() {
        if (activeLoggingFile.exists()) {
            PrintWriter(activeLoggingFile).use { writer ->
                writer.print("")
            }
        }
    }

    private fun writeLogEntry(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (!isStarted.get()) return
        val channel = logCommandChannel ?: return

        val lines = buildList {
            add("${timestampFormatter.get()!!.now()} $severity: ($tag) $message")
        }
        channel.trySend(LogCommand.Entry(lines, throwable))
    }

    private fun appendLogLine(text: String) {
        logBuffer.add(text)
    }

    private suspend fun processLogCommands(channel: Channel<LogCommand>) {
        var lastFlushTime = System.currentTimeMillis()
        try {
            while (currentCoroutineContext().isActive) {
                val command = receiveLogCommand(channel, lastFlushTime)

                if (command == null) {
                    lastFlushTime = tryFlushBufferAndGetTime(lastFlushTime)
                    continue
                }

                val result = handleLogCommand(command, lastFlushTime)
                lastFlushTime = result.lastFlushTime
                if (result.shouldStop) return
            }
        } finally {
            closeResources()
        }
    }

    private suspend fun receiveLogCommand(channel: Channel<LogCommand>, lastFlushTime: Long): LogCommand? {
        val flushTimeout = (config.flushIntervalMs - (System.currentTimeMillis() - lastFlushTime))
            .coerceAtLeast(0L)
        return withTimeoutOrNull(flushTimeout) { channel.receive() }
    }

    private fun handleLogCommand(command: LogCommand, lastFlushTime: Long): LogCommandResult = when (command) {
        is LogCommand.Entry -> LogCommandResult(handleEntryCommand(command, lastFlushTime))
        is LogCommand.Flush -> LogCommandResult(completeFlushCommand(command, lastFlushTime))
        is LogCommand.DeleteAll -> LogCommandResult(completeDeleteAllCommand(command, lastFlushTime))
        is LogCommand.Stop -> {
            completeStopCommand(command)
            LogCommandResult(lastFlushTime = lastFlushTime, shouldStop = true)
        }
    }

    private fun handleEntryCommand(command: LogCommand.Entry, lastFlushTime: Long): Long {
        var updatedFlushTime = lastFlushTime
        try {
            command.lines.forEach(::appendLogLine)
            command.throwable?.let { appendLogLine(it.stackTraceToString()) }
            if (logBuffer.size >= config.maxBufferSize) {
                updatedFlushTime = flushBufferAndGetTime()
            }
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Failed to write log entry", e)
        }
        return updatedFlushTime
    }

    private fun completeFlushCommand(command: LogCommand.Flush, lastFlushTime: Long): Long {
        var updatedFlushTime = lastFlushTime
        completeCommand(command.completion) {
            updatedFlushTime = flushBufferAndGetTime()
        }
        return updatedFlushTime
    }

    private fun completeDeleteAllCommand(command: LogCommand.DeleteAll, lastFlushTime: Long): Long {
        var updatedFlushTime = lastFlushTime
        completeCommand(command.completion) {
            deleteAllLogFilesInternal()
            updatedFlushTime = System.currentTimeMillis()
        }
        return updatedFlushTime
    }

    private fun completeStopCommand(command: LogCommand.Stop) {
        completeCommand(command.completion) {
            logBuffer.clear()
            closeResources()
            clearActiveLoggingFileContent()
        }
    }

    private fun flushBufferAndGetTime(): Long {
        flushBuffer()
        return System.currentTimeMillis()
    }

    private fun tryFlushBufferAndGetTime(lastFlushTime: Long): Long = try {
        flushBufferAndGetTime()
    } catch (e: IOException) {
        Log.e(LOG_TAG, "Failed to flush log buffer", e)
        lastFlushTime
    }

    private inline fun completeCommand(completion: CompletableDeferred<Unit>, block: () -> Unit) {
        try {
            block()
            completion.complete(Unit)
        } catch (e: Exception) {
            completion.completeExceptionally(e)
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

    override suspend fun deleteAllLogFiles() {
        val channel = logCommandChannel
        if (isStarted.get() && channel != null) {
            val completion = CompletableDeferred<Unit>()
            withTimeout(config.flushTimeoutMs) {
                channel.send(LogCommand.DeleteAll(completion))
                completion.await()
            }
            return
        }

        withContext(Dispatchers.IO) {
            deleteAllLogFilesInternal()
        }
    }

    private fun deleteAllLogFilesInternal() {
        logBuffer.clear()
        closeResources()
        clearActiveLoggingFileContent()
        logsDirectory.listFiles()?.filter {
            it.extension.lowercase(Locale.ROOT) == LOG_COMPRESSED_FILE_EXTENSION
        }?.forEach { it.delete() }
    }

    @Throws(IOException::class)
    private fun flushBuffer() {
        if (logBuffer.isEmpty()) return

        val writer = bufferedWriter ?: BufferedWriter(
            FileWriter(activeLoggingFile, true),
            config.bufferSizeBytes
        ).also { bufferedWriter = it }

        logBuffer.forEach { line ->
            writer.appendLine(line)
        }
        writer.flush()

        logBuffer.clear()
    }

    private sealed interface LogCommand {
        data class Entry(val lines: List<String>, val throwable: Throwable?) : LogCommand
        data class Flush(val completion: CompletableDeferred<Unit>) : LogCommand
        data class DeleteAll(val completion: CompletableDeferred<Unit>) : LogCommand
        data class Stop(val completion: CompletableDeferred<Unit>) : LogCommand
    }

    private data class LogCommandResult(
        val lastFlushTime: Long,
        val shouldStop: Boolean = false
    )

    companion object {
        private const val LOG_TAG = "LogFileWriter"
        private const val LOG_FILE_PREFIX = "wire"
        private const val ACTIVE_LOGGING_FILE_NAME = "${LOG_FILE_PREFIX}_logs.txt"
        private const val LOG_COMPRESSED_FILE_EXTENSION = "gz"
        private const val LOG_COMMAND_CHANNEL_CAPACITY = 10_000
    }
}
