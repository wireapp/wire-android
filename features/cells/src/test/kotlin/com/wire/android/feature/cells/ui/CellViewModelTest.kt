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

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
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
import com.wire.kalium.common.functional.right
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

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

        val items = viewModel.nodesFlow.asSnapshot()
        assertEquals(items.size, 2)

        coVerify(exactly = 1) { arrangement.getCellFilesPagedUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is present file is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        viewModel.sendIntent(CellViewIntent.OnItemClick(testFiles[0].toUiModel()))

        coVerify(exactly = 1) { arrangement.fileExternalActions.openLocalFile(any(), any(), any(), any()) }
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

        coVerify(exactly = 1) { arrangement.fileExternalActions.openUrl(any(), any(), any()) }
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
        coVerify(exactly = 1) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given share action selected for local file then file is shared`() = runTest {
        val testFile = testFiles[0].toUiModel()
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withShareActionResult(testFile)
            .arrange()

        viewModel.sendIntent(CellViewIntent.OnMenuItemActionSelected(testFile, NodeBottomSheetAction.SHARE))

        coVerify(exactly = 1) {
            arrangement.fileExternalActions.shareLocalFile(
                localPath = testFile.localPath!!,
                assetName = testFile.name,
                mimeType = testFile.mimeType,
                onError = any(),
            )
        }
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

        coVerify(exactly = 0) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { arrangement.fileExternalActions.openLocalFile(any(), any(), any(), any()) }
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

        with(viewModel.nodesFlow.asSnapshot()) {
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

    private class Arrangement(private val conversationId: String? = null) {

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
        lateinit var fileExternalActions: CellFileExternalActions

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

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { isCellAvailableUseCase.invoke() } returns true.right()

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
            coEvery { downloadCellFileUseCase(any(), any(), any(), any(), any()) } returns Unit.right()
        }

        fun withSlowDownloadSuccess() = apply {
            coEvery { downloadCellFileUseCase(any(), any(), any(), any(), any()) } coAnswers {
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

        fun withShareActionResult(file: CellNodeUi.File) = apply {
            every {
                cellFileActionsMenu.onMenuItemAction(
                    conversationId = any(),
                    parentFolderUuid = any(),
                    node = file,
                    action = NodeBottomSheetAction.SHARE,
                    onResult = any(),
                )
            } answers {
                @Suppress("UNCHECKED_CAST")
                val onResult = invocation.args[4] as (CellFileActionsMenu.MenuActionResult) -> Unit
                onResult(CellFileActionsMenu.Share(file))
            }
        }

        fun arrange(): Pair<Arrangement, CellViewModel> {

            every { fileHelper.getCacheDir() } returns File("")
            every { fileNameResolver.getUniqueFile(any(), any()) } returns File("")

            coEvery { getWireCellsConfig() } returns null

            val openFileDownloadController = OpenFileDownloadController(
                download = downloadCellFileUseCase,
                fileHelper = fileHelper,
                fileNameResolver = fileNameResolver,
                sharedPathCache = sharedPathCache,
            )

            return this to CellViewModel(
                navArgs = CellFilesNavArgs(conversationId = conversationId),
                searchNavArgs = null,
                getCellFilesPaged = getCellFilesPagedUseCase,
                deleteCellAsset = deleteCellAssetUseCase,
                restoreNodeFromRecycleBinUseCase = restoreNodeFromRecycleBinUseCase,
                isCellAvailable = isCellAvailableUseCase,
                fileExternalActions = fileExternalActions,
                onlineEditor = onlineEditor,
                getEditorUrl = getEditorUrlUseCase,
                cellFileActionsMenu = cellFileActionsMenu,
                getWireCellsConfig = getWireCellsConfig,
                sharedPathCache = sharedPathCache,
                openFileDownloadController = openFileDownloadController,
            )
        }
    }
}
