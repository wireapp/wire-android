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

data class LogFileWriterV1Config(
    val rollOnSizeBytes: Long = DEFAULT_ROLL_ON_SIZE_BYTES,
    val maxLogFiles: Int = DEFAULT_MAX_LOG_FILES,
    val flushTimeoutMs: Long = DEFAULT_FLUSH_TIMEOUT_MS,
) {
    companion object {
        private const val DEFAULT_ROLL_ON_SIZE_BYTES = 25 * 1024 * 1024L

        // RollingFileLogWriter counts the active file, so this retains ten rolls.
        private const val DEFAULT_MAX_LOG_FILES = 11
        private const val DEFAULT_FLUSH_TIMEOUT_MS = 5000L

        fun default() = LogFileWriterV1Config()
    }
}
