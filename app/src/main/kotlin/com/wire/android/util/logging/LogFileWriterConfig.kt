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

data class LogFileWriterConfig(
    val rollOnSizeBytes: Long = DEFAULT_ROLL_ON_SIZE_BYTES,
    val maxLogFiles: Int = DEFAULT_MAX_LOG_FILES,
    val flushTimeoutMs: Long = DEFAULT_FLUSH_TIMEOUT_MS,
) {
    companion object {
        private const val DEFAULT_ROLL_ON_SIZE_BYTES = 25 * 1024 * 1024L // 25 MiB

        // RollingFileLogWriter counts the active file, so this retains ten rolled files.
        private const val DEFAULT_MAX_LOG_FILES = 11
        private const val DEFAULT_FLUSH_TIMEOUT_MS = 5000L // 5 seconds

        fun default() = LogFileWriterConfig()
    }
}
