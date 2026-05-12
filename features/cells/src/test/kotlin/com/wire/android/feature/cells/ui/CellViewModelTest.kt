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
package com.wire.android.feature.cells.ui

import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.ramcosta.composedestinations.generated.cells.destinations.ConversationFilesScreenDestination
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.android.feature.cells.util.FileNameResolver
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.cells.domain.usecase.IsAtLeastOneCellAvailableUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.cells.domain.usecase.download.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.DeleteOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.GetOfflineFileUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.common.functional.right
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(NavigationTestExtension::class)
class CellViewModelTest {

    private companion object {
        val testFiles = listOf(
            Node.File(
                uuid = "fileUuid",
                versionId = "versionId",
                name = "fileName",
                mimeType = "image/png",
                remotePath = "remotePath",
                localPath = "localPath",
                contentUrl = "https://example.com/file",
                size = 1024,
                modifiedTime = 1234567890L,
            ),
            Node.File(
                uuid = "fileUuid2",
                versionId = "versionId2",
                name = "fileName2",
                mimeType = "image/png",
                remotePath = "remotePath2",
                localPath = null,
                contentUrl = null,
                size = 2048,
                modifiedTime = 1234567890L,
            )
        )
        val localFilePath = "localPath".toPath()
    }

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
    fun `given view model when files flow subscribed cell files are loaded`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val pagingData = viewModel.nodesFlow.first()
        val items = flowOf(pagingData).asSnapshot()
        assertEquals(items.size, 2)

        coVerify(exactly = 1) { arrangement.getCellFilesPagedUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is present file is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        viewModel.sendIntent(CellViewIntent.OnItemClick(testFiles[0].toUiModel()))

        coVerify(exactly = 1) { arrangement.fileHelper.openAssetFileWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is not present and url is openable then url is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val testFile = testFiles[0].copy(
            localPath = null,
            contentUrl = "https://example.com/file"
        )

        viewModel.sendIntent(CellViewIntent.OnItemClick(testFile.toUiModel()))

        coVerify(exactly = 1) { arrangement.fileHelper.openAssetUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is not present and url is not openable then download starts immediately`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDownloadSuccess()
            .arrange()

        val testFile = testFiles[0].copy(
            localPath = null,
            contentUrl = null
        ).toUiModel()

        viewModel.sendIntent(CellViewIntent.OnItemClick(testFile))
        // Advance time so download coroutine can complete
        advanceUntilIdle()

        // Download use case was called
        coVerify(exactly = 1) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given file has local path in DB when clicked with error state then file opened without re-downloading`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        // File has localPath from DB but also carries an error state (stale UI state)
        val testFile = testFiles[0].copy(localPath = "localPath", contentUrl = null).toUiModel()
            .copy(openLoadState = OpenLoadState.Error)

        viewModel.sendIntent(CellViewIntent.OnItemClick(testFile))
        advanceUntilIdle()

        coVerify(exactly = 0) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.fileHelper.openAssetFileWithExternalApp(any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when file tap triggers slow download then file ready event is emitted to shared cache`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withSlowDownloadSuccess()
            .arrange()

        val testFile = testFiles[0].copy(
            localPath = null,
            contentUrl = null
        ).toUiModel()

        arrangement.sharedPathCache.fileReadyEvents.test {
            viewModel.sendIntent(CellViewIntent.OnItemClick(testFile))
            advanceUntilIdle()

            val file = awaitItem()
            assertEquals(testFile.uuid, file.uuid)
        }
    }

    @Test
    fun `given view model when delete is confirmed then file is removed from the list`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDeleteSuccess()
            .arrange()

        val testFile = testFiles[0]
            .toUiModel()

        viewModel.sendIntent(CellViewIntent.OnNodeDeleteConfirmed(testFile))
        advanceUntilIdle()

        // nodesFlow is hot — take the current PagingData and wrap it for asSnapshot() to terminate.
        val pagingData = viewModel.nodesFlow.first()
        with(flowOf(pagingData).asSnapshot()) {
            assertFalse(contains(testFile))
        }
    }

    @Test
    fun `given view model when delete is confirmed then file is deleted on server`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDeleteSuccess()
            .arrange()

        val testFile = testFiles[0].toUiModel()

        viewModel.sendIntent(CellViewIntent.OnNodeDeleteConfirmed(testFile))

        coVerify(exactly = 1) {
            arrangement.deleteCellAssetUseCase(any(), any())
        }
    }

    @Test
    fun `GIVEN no cells available WHEN ViewModel initialized THEN no load request is sent`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNoCellsAvailable()
            .arrange()

        viewModel.nodesFlow.test {
            coVerify(exactly = 0) { arrangement.getCellFilesPagedUseCase(any(), any(), any(), any()) }
            cancelAndConsumeRemainingEvents()
        }
    }

    private class Arrangement(conversationId: String? = null) {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getCellFilesPagedUseCase: GetPaginatedFilesFlowUseCase

        @MockK
        lateinit var deleteCellAssetUseCase: DeleteCellAssetUseCase

        @MockK
        lateinit var downloadCellFileUseCase: DownloadCellFileUseCase

        @MockK
        lateinit var restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase

        @MockK
        lateinit var isCellAvailableUseCase: IsAtLeastOneCellAvailableUseCase

        @MockK
        lateinit var fileHelper: FileHelper

        @MockK
        lateinit var fileNameResolver: FileNameResolver

        val sharedPathCache = CellFileLocalPathCache()

        @MockK
        lateinit var getEditorUrlUseCase: GetEditorUrlUseCase

        @MockK
        lateinit var onlineEditor: OnlineEditor

        @MockK
        lateinit var cellFileActionsMenu: CellFileActionsMenu

        @MockK
        lateinit var getWireCellsConfig: GetWireCellConfigurationUseCase

        @MockK
        lateinit var observeOfflineFiles: ObserveOfflineFilesUseCase

        @MockK
        lateinit var deleteOfflineFile: DeleteOfflineFileUseCase

        @MockK
        lateinit var getOfflineFile: GetOfflineFileUseCase

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            mockkObject(ConversationFilesScreenDestination)
            every { ConversationFilesScreenDestination.argsFrom(savedStateHandle) } returns CellFilesNavArgs(
                conversationId = conversationId
            )

            every { savedStateHandle.get<String>(any()) } returns conversationId
            every { savedStateHandle.get<String>("conversationId") } returns conversationId

            coEvery { isCellAvailableUseCase.invoke() } returns true.right()

            every { observeOfflineFiles() } returns flowOf(emptyList())
            coEvery { getOfflineFile(any()) } returns null

            coEvery { getCellFilesPagedUseCase.invoke(any(), any(), any(), any()) } returns flowOf(
                PagingData.from(
                    data = listOf(
                        testFiles[0]
                    ),
                    sourceLoadStates = LoadStates(
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                        refresh = LoadState.NotLoading(true),
                    ),
                )
            )
        }

        fun withLoadSuccess() = apply {
            coEvery { getCellFilesPagedUseCase(any(), any(), any(), any()) } returns flowOf(
                PagingData.from(
                    data = testFiles,
                    sourceLoadStates = LoadStates(
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                        refresh = LoadState.NotLoading(true),
                    ),
                )
            )
        }

        fun withDownloadSuccess() = apply {
            coEvery { downloadCellFileUseCase(any(), any(), any(), any(), any(), any(), any()) } returns Unit.right()
        }

        fun withSlowDownloadSuccess() = apply {
            coEvery { downloadCellFileUseCase(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
                delay(500) // Simulate download taking 500ms (longer than the 300ms threshold)
                Unit.right()
            }
        }

        fun withDeleteSuccess() = apply {
            coEvery { deleteCellAssetUseCase(any(), any()) } returns Unit.right()
        }

        fun withNoCellsAvailable() = apply {
            coEvery { isCellAvailableUseCase.invoke() } returns false.right()
        }

        fun arrange(): Pair<Arrangement, CellViewModel> {

            every { fileHelper.getExternalFilesDir() } returns File("")
            every { fileNameResolver.getUniqueFile(any(), any()) } returns File("")

            coEvery { getWireCellsConfig() } returns null

            val openFileDownloadController = OpenFileDownloadController(
                download = downloadCellFileUseCase,
                fileHelper = fileHelper,
                fileNameResolver = fileNameResolver,
                sharedPathCache = sharedPathCache,
            )

            val offlineFileDownloadController = OfflineFileDownloadController(
                download = downloadCellFileUseCase,
                fileHelper = fileHelper,
                fileNameResolver = fileNameResolver,
                saveOfflineFile = mockk(relaxUnitFun = true),
                sharedPathCache = sharedPathCache,
            )

            return this to CellViewModel(
                savedStateHandle = savedStateHandle,
                getCellFilesPaged = getCellFilesPagedUseCase,
                deleteCellAsset = deleteCellAssetUseCase,
                restoreNodeFromRecycleBinUseCase = restoreNodeFromRecycleBinUseCase,
                isCellAvailable = isCellAvailableUseCase,
                fileHelper = fileHelper,
                onlineEditor = onlineEditor,
                getEditorUrl = getEditorUrlUseCase,
                cellFileActionsMenu = cellFileActionsMenu,
                getWireCellsConfig = getWireCellsConfig,
                sharedPathCache = sharedPathCache,
                openFileDownloadController = openFileDownloadController,
                offlineFileDownloadController = offlineFileDownloadController,
                observeOfflineFiles = observeOfflineFiles,
                deleteOfflineFile = deleteOfflineFile,
                getOfflineFile = getOfflineFile,
            )
        }
    }
}
