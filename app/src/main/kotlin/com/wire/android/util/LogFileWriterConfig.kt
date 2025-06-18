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

package com.wire.android.util

data class LogFileWriterConfig(
    val flushIntervalMs: Long = DEFAULT_FLUSH_INTERVAL_MS,
    val maxBufferSize: Int = DEFAULT_MAX_BUFFER_SIZE,
    val bufferSizeBytes: Int = DEFAULT_BUFFER_SIZE_BYTES,
    val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
    val enableAsyncFlushing: Boolean = true
) {
    companion object {
        private const val DEFAULT_FLUSH_INTERVAL_MS = 5000L
        private const val DEFAULT_MAX_BUFFER_SIZE = 100
        private const val DEFAULT_BUFFER_SIZE_BYTES = 64 * 1024
        private const val DEFAULT_MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L // 25MB

        fun default() = LogFileWriterConfig()
    }
}
