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
package com.wire.android.feature.cells.ui

/**
 * Tracks the downloads started by one "make available offline" bulk action, so a single outcome is reported
 * through [onFinished] once all of them settled, instead of one message per file.
 *
 * Cancelled downloads count as settled but neither saved nor failed.
 */
internal class BulkOfflineProgress(
    private val total: Int,
    private val onFinished: (Result) -> Unit,
) {
    private var remaining = total
    private var savedCount = 0
    private var error: CellError? = null

    @Synchronized
    fun onSaved() {
        savedCount++
        settle()
    }

    @Synchronized
    fun onFailed(error: CellError) {
        // Running out of space is the more useful thing to tell the user about.
        if (this.error == null || error == CellError.NO_SPACE_LEFT) this.error = error
        settle()
    }

    @Synchronized
    fun onCancelled() = settle()

    private fun settle() {
        remaining--
        if (remaining > 0) return
        val result = when (val error = error) {
            CellError.NO_SPACE_LEFT -> Result.RanOutOfStorage(savedCount = savedCount, totalCount = total)
            null -> if (savedCount > 0) Result.Saved(savedCount) else null
            else -> Result.Failed(error)
        }
        result?.let(onFinished)
    }

    sealed interface Result {
        data class Saved(val savedCount: Int) : Result
        data class Failed(val error: CellError) : Result
        data class RanOutOfStorage(val savedCount: Int, val totalCount: Int) : Result
    }
}
