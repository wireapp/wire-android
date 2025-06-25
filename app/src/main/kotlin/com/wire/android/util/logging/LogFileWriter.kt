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

import android.content.Context
import java.io.File

/**
 * Common interface for log file writers to enable easy substitution
 * between different implementations.
 */
interface LogFileWriter {

    /**
     * The active logging file where logs are currently being written
     */
    val activeLoggingFile: File

    /**
     * Starts the log collection system
     */
    suspend fun start()

    /**
     * Stops the log collection system
     */
    suspend fun stop()

    /**
     * Forces a flush of any pending logs to ensure they are written to file
     */
    suspend fun forceFlush()

    /**
     * Deletes all log files including active and compressed files
     *
     */
    fun deleteAllLogFiles()

    companion object {
        fun logsDirectory(context: Context) = File(context.cacheDir, "logs")
    }
}
