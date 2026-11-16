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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BulkOfflineProgressTest {

    private var result: BulkOfflineProgress.Result? = null
    private var finishedCount = 0

    private fun progress(total: Int) = BulkOfflineProgress(total) {
        result = it
        finishedCount++
    }

    @Test
    fun `given every file saved when the last one settles then the saved count is reported`() {
        val progress = progress(total = 3)

        repeat(3) { progress.onSaved() }

        assertEquals(BulkOfflineProgress.Result.Saved(savedCount = 3), result)
    }

    @Test
    fun `given files still downloading when some settled then nothing is reported yet`() {
        val progress = progress(total = 3)

        progress.onSaved()
        progress.onFailed(CellError.DOWNLOAD_FAILED)

        assertNull(result)
    }

    @Test
    fun `given every file settled when the outcome is reported then it is reported once`() {
        val progress = progress(total = 2)

        progress.onSaved()
        progress.onSaved()

        assertEquals(1, finishedCount)
    }

    @Test
    fun `given some files cancelled when the others are saved then only the saved ones are counted`() {
        val progress = progress(total = 3)

        progress.onSaved()
        progress.onCancelled()
        progress.onSaved()

        assertEquals(BulkOfflineProgress.Result.Saved(savedCount = 2), result)
    }

    @Test
    fun `given every file cancelled when the last one settles then nothing is reported`() {
        val progress = progress(total = 2)

        progress.onCancelled()
        progress.onCancelled()

        assertNull(result)
        assertEquals(0, finishedCount)
    }

    @Test
    fun `given a file failed when the others are saved then the failure is reported`() {
        val progress = progress(total = 2)

        progress.onSaved()
        progress.onFailed(CellError.DOWNLOAD_FAILED)

        assertEquals(BulkOfflineProgress.Result.Failed(CellError.DOWNLOAD_FAILED), result)
    }

    @Test
    fun `given the device ran out of space when another failure comes after then running out of space is reported`() {
        val progress = progress(total = 3)

        progress.onSaved()
        progress.onFailed(CellError.NO_SPACE_LEFT)
        progress.onFailed(CellError.DOWNLOAD_FAILED)

        assertEquals(BulkOfflineProgress.Result.RanOutOfStorage(savedCount = 1, totalCount = 3), result)
    }

    @Test
    fun `given another failure first when the device then runs out of space then running out of space is reported`() {
        val progress = progress(total = 2)

        progress.onFailed(CellError.DOWNLOAD_FAILED)
        progress.onFailed(CellError.NO_SPACE_LEFT)

        assertEquals(BulkOfflineProgress.Result.RanOutOfStorage(savedCount = 0, totalCount = 2), result)
    }
}
