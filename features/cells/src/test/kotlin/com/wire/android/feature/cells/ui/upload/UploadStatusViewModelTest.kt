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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UploadStatusViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given coordinator uploads in this conversation, when observed, then they are exposed`() {
        val uploads = listOf(uploadItem("1", conversationId = "convA"), uploadItem("2", conversationId = "convA"))
        val (_, viewModel) = Arrangement(conversationId = "convA")
            .withUploads(uploads)
            .arrange()

        assertEquals(uploads, viewModel.uploads.value)
    }

    @Test
    fun `given coordinator uploads in another conversation, when observed, then they are filtered out`() {
        val uploads = listOf(uploadItem("1", conversationId = "convA"), uploadItem("2", conversationId = "convB"))
        val (_, viewModel) = Arrangement(conversationId = "convA")
            .withUploads(uploads)
            .arrange()

        assertEquals(listOf(uploads[0]), viewModel.uploads.value)
    }

    @Test
    fun `given an id, when cancel is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.cancel("id1")

        verify(exactly = 1) { arrangement.coordinator.cancel("id1") }
    }

    @Test
    fun `when cancelAll is called, then it forwards to the coordinator scoped to this conversation`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.cancelAll()

        verify(exactly = 1) { arrangement.coordinator.cancelAll("convA") }
        verify(exactly = 0) { arrangement.coordinator.cancelAll() }
    }

    @Test
    fun `given an id, when retry is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.retry("id1")

        verify(exactly = 1) { arrangement.coordinator.retry("id1") }
    }

    @Test
    fun `when retryAllFailed is called, then it forwards to the coordinator scoped to this conversation`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.retryAllFailed()

        verify(exactly = 1) { arrangement.coordinator.retryAllFailed("convA") }
        verify(exactly = 0) { arrangement.coordinator.retryAllFailed() }
    }

    @Test
    fun `given an id, when dismiss is called, then it forwards to the coordinator`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.dismiss("id1")

        verify(exactly = 1) { arrangement.coordinator.dismiss("id1") }
    }

    @Test
    fun `when dismissAll is called, then it forwards to the coordinator scoped to this conversation`() {
        val (arrangement, viewModel) = Arrangement(conversationId = "convA").arrange()

        viewModel.dismissAll()

        verify(exactly = 1) { arrangement.coordinator.dismissAll("convA") }
        verify(exactly = 0) { arrangement.coordinator.dismissAll() }
    }

    @Test
    fun `given no conversation context, when bulk actions are called, then nothing is forwarded to the coordinator`() {
        val (arrangement, viewModel) = Arrangement(conversationId = null).arrange()

        viewModel.cancelAll()
        viewModel.retryAllFailed()
        viewModel.dismissAll()

        verify(exactly = 0) { arrangement.coordinator.cancelAll(any()) }
        verify(exactly = 0) { arrangement.coordinator.retryAllFailed(any()) }
        verify(exactly = 0) { arrangement.coordinator.dismissAll(any()) }
    }

    private fun uploadItem(id: String, conversationId: String) = CellUploadItem(
        id = id,
        conversationId = conversationId,
        request = CellUploadRequest(
            localPath = "/path/to/file$id.txt".toPath(),
            fileName = "file$id.txt",
            sizeBytes = 1024,
            destinationFolderPath = "$conversationId/folder",
        ),
        state = CellUploadState.Queued,
    )

    private class Arrangement(private val conversationId: String?) {

        @MockK
        lateinit var coordinator: CellUploadCoordinator

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coordinator.uploads } returns MutableStateFlow(emptyList())
        }

        private val viewModel by lazy { UploadStatusViewModel(conversationId, coordinator) }

        fun withUploads(uploads: List<CellUploadItem>) = apply {
            every { coordinator.uploads } returns MutableStateFlow(uploads)
        }

        fun arrange() = this to viewModel
    }
}