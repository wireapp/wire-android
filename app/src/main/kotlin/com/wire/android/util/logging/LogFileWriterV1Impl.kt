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

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import java.io.File
import java.io.IOException
import java.util.UUID

/** Persists diagnostics directly from Kermit instead of reading device logcat. */
class LogFileWriterV1Impl(
    private val logsDirectory: File,
    private val config: LogFileWriterV1Config = LogFileWriterV1Config.default(),
) : LogFileWriter {

    override val activeLoggingFile = File(logsDirectory, "$LOG_FILE_NAME.log")

    private var rollingWriter: RollingFileLogWriter? = null
    private val gatedWriter = GatedLogWriter { rollingWriter }

    override val logWriter: LogWriter = gatedWriter

    override suspend fun start() = withContext(Dispatchers.IO) {
        ensureLogDirectoryExists()
        if (rollingWriter == null) {
            rollingWriter = RollingFileLogWriter(
                RollingFileLogWriterConfig(
                    logFileName = LOG_FILE_NAME,
                    logFilePath = Path(logsDirectory.absolutePath),
                    rollOnSize = config.rollOnSizeBytes,
                    maxLogFiles = config.maxLogFiles,
                )
            )
        }
        gatedWriter.enable()
    }

    override suspend fun stop() {
        gatedWriter.disable()
    }

    override suspend fun forceFlush() {
        gatedWriter.flushBarrier()?.let { waitForBarrier(it) }
    }

    override suspend fun deleteAllLogFiles() {
        val marker = gatedWriter.flushBarrier(disableAfterWrite = true)
        marker?.let { waitForBarrier(it) }

        withContext(Dispatchers.IO) {
            logsDirectory.listFiles()
                ?.filter { ROLLED_LOG_FILE_REGEX.matches(it.name) }
                ?.forEach(File::delete)
            ensureLogDirectoryExists()
            activeLoggingFile.outputStream().use { }
        }

        if (marker != null) gatedWriter.enable()
    }

    private suspend fun waitForBarrier(marker: String) = withContext(Dispatchers.IO) {
        withTimeout(config.flushTimeoutMs) {
            while (!containsBarrier(marker)) delay(FLUSH_POLL_INTERVAL_MS)
        }
    }

    private fun containsBarrier(marker: String): Boolean = logsDirectory.listFiles()
        ?.filter { it.name == activeLoggingFile.name || ROLLED_LOG_FILE_REGEX.matches(it.name) }
        ?.any { file -> runCatching { file.useLines { lines -> lines.any { marker in it } } }.getOrDefault(false) }
        ?: false

    private fun ensureLogDirectoryExists() {
        if (!logsDirectory.exists() && !logsDirectory.mkdirs()) {
            throw IOException("Unable to create log directory: ${logsDirectory.absolutePath}")
        }
    }

    private class GatedLogWriter(private val delegate: () -> LogWriter?) : LogWriter() {
        private val lock = Any()
        private var enabled = false

        fun enable() = synchronized(lock) { enabled = true }

        fun disable() = synchronized(lock) { enabled = false }

        fun flushBarrier(disableAfterWrite: Boolean = false): String? = synchronized(lock) {
            if (!enabled) return null

            val marker = "$FLUSH_MARKER_PREFIX${UUID.randomUUID()}"
            delegate()?.log(Severity.Verbose, marker, LOG_TAG, null)
            if (disableAfterWrite) enabled = false
            marker
        }

        override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
            synchronized(lock) {
                if (enabled) delegate()?.log(severity, message, tag, throwable)
            }
        }
    }

    private companion object {
        const val LOG_FILE_NAME = "wire_logs"
        const val LOG_TAG = "LogFileWriter"
        const val FLUSH_MARKER_PREFIX = "wire-log-flush:"
        const val FLUSH_POLL_INTERVAL_MS = 10L
        val ROLLED_LOG_FILE_REGEX = Regex("$LOG_FILE_NAME-[0-9]+\\.log")
    }
}
