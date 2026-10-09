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
package com.wire.android.feature.cells.ui.upload

import com.wire.kalium.cells.domain.CellUploadCoordinator
import com.wire.kalium.cells.domain.CellUploadItem
import com.wire.kalium.cells.domain.CellUploadRequest
import com.wire.kalium.cells.domain.CellUploadState
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UploadStatusViewModelTest {

    @Test
    fun `given coordinator uploads, when observed, then it exposes the same value`() {
        val uploads = listOf(uploadItem("1"), uploadItem("2"))
        val (_, viewModel) = Arrangement()
            .withUploads(uploads)
            .arrange()

        assertEquals(uploads, viewModel.uploads.value)
    }

    @Test
    fun `given an id, when cancel is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.cancel("id1")

        verify(exactly = 1) { arrangement.coordinator.cancel("id1") }
    }

    @Test
    fun `when cancelAll is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.cancelAll()

        verify(exactly = 1) { arrangement.coordinator.cancelAll() }
    }

    @Test
    fun `given an id, when retry is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.retry("id1")

        verify(exactly = 1) { arrangement.coordinator.retry("id1") }
    }

    @Test
    fun `when retryAllFailed is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.retryAllFailed()

        verify(exactly = 1) { arrangement.coordinator.retryAllFailed() }
    }

    @Test
    fun `given an id, when dismiss is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.dismiss("id1")

        verify(exactly = 1) { arrangement.coordinator.dismiss("id1") }
    }

    @Test
    fun `when dismissAll is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.dismissAll()

        verify(exactly = 1) { arrangement.coordinator.dismissAll() }
    }

    private fun uploadItem(id: String) = CellUploadItem(
        id = id,
        request = CellUploadRequest(
            localPath = "/path/to/file$id.txt".toPath(),
            fileName = "file$id.txt",
            sizeBytes = 1024,
            destinationFolderPath = "cellName/folder",
        ),
        state = CellUploadState.Queued,
    )

    private class Arrangement {

        @MockK
        lateinit var coordinator: CellUploadCoordinator

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coordinator.uploads } returns MutableStateFlow(emptyList())
        }

        private val viewModel by lazy { UploadStatusViewModel(coordinator) }

        fun withUploads(uploads: List<CellUploadItem>) = apply {
            every { coordinator.uploads } returns MutableStateFlow(uploads)
        }

        fun arrange() = this to viewModel
    }
}
