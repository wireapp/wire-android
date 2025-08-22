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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.cells.ui.model.NodeBottomSheetAction
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.DeleteCellAssetUseCase
import com.wire.kalium.cells.domain.usecase.DownloadCellFileUseCase
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.RestoreNodeFromRecycleBinUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

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
    fun `given view model when files flow subscibed cell files are loaded`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val items = viewModel.nodesFlow.asSnapshot()
        assertEquals(items.size, 2)

        coVerify(exactly = 1) { arrangement.getCellFilesPagedUseCase(any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is present file is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        viewModel.sendIntent(CellViewIntent.OnFileClick(testFiles[0].toUiModel()))

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

        viewModel.sendIntent(CellViewIntent.OnFileClick(testFile.toUiModel()))

        coVerify(exactly = 1) { arrangement.fileHelper.openAssetUrlWithExternalApp(any(), any(), any()) }
    }

    @Test
    fun `given view model when file clicked and local file is not present and url is not openable then download dialog shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val testFile = testFiles[0].copy(
            localPath = null,
            contentUrl = null
        ).toUiModel()

        viewModel.downloadFileSheet.test {
            viewModel.sendIntent(CellViewIntent.OnFileClick(testFile))

            with(expectMostRecentItem()) {
                assertEquals(testFile, this)
            }
        }
    }

    @Test
    fun `given view model when download confirmed then file is downloaded`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDownloadSuccess()
            .arrange()

        viewModel.sendIntent(CellViewIntent.OnFileDownloadConfirmed(testFiles[0].toUiModel()))

        coVerify(exactly = 1) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when download confirmed and download fails then error is emitted`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDownloadFailure()
            .arrange()

        viewModel.actions.test {

            viewModel.sendIntent(CellViewIntent.OnFileDownloadConfirmed(testFiles[0].toUiModel()))

            with(expectMostRecentItem()) {
                assertTrue(this is ShowError)
            }
        }

        coVerify(exactly = 1) { arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given view model when file menu clicked and local file available then file menu is opened`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        viewModel.menu.filterIsInstance(MenuOptions::class).test {
            val testFile = testFiles[0].toUiModel()

            viewModel.sendIntent(CellViewIntent.OnItemMenuClick(testFile))

            with(expectMostRecentItem()) {
                assertEquals(testFile, node)
                assertEquals(NodeBottomSheetAction.SHARE, actions.first())
            }
        }
    }

    @Test
    fun `given view model when file menu clicked and local file not available then file menu is opened`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        viewModel.menu.filterIsInstance(MenuOptions::class).test {

            val testFile = testFiles[0]
                .toUiModel()
                .copy(localPath = null)

            viewModel.sendIntent(CellViewIntent.OnItemMenuClick(testFile))

            with(expectMostRecentItem()) {
                assertEquals(testFile, node)
                assertEquals(NodeBottomSheetAction.DOWNLOAD, actions[1])
            }
        }
    }

    @Test
    fun `given view model when save menu action selected then download starts`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .withDownloadSuccess()
            .arrange()

        val testFile = testFiles[0]
            .toUiModel()
            .copy(localPath = null)

        viewModel.sendIntent(CellViewIntent.OnMenuItemActionSelected(testFile, NodeBottomSheetAction.DOWNLOAD))

        coVerify(exactly = 1) {
            arrangement.downloadCellFileUseCase(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `given view model when share menu action selected then file is shared`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val testFile = testFiles[0]
            .toUiModel()

        viewModel.sendIntent(CellViewIntent.OnMenuItemActionSelected(testFile, NodeBottomSheetAction.SHARE))

        coVerify(exactly = 1) {
            arrangement.fileHelper.shareFileChooser(any(), any(), any(), any())
        }
    }

    @Test
    fun `given view model when public link menu action selected then public link is created`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val testFile = testFiles[0]
            .toUiModel()

        viewModel.actions.test {
            viewModel.sendIntent(CellViewIntent.OnMenuItemActionSelected(testFile, NodeBottomSheetAction.PUBLIC_LINK))

            with(expectMostRecentItem()) {
                assertTrue(this is ShowPublicLinkScreen)
                assertEquals(testFile, (this as ShowPublicLinkScreen).cellNode)
            }
        }
    }

    @Test
    fun `given view model when delete menu action selected then delete confirmation is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withLoadSuccess()
            .arrange()

        val testFile = testFiles[0]
            .toUiModel()

        viewModel.actions.test {
            viewModel.sendIntent(CellViewIntent.OnMenuItemActionSelected(testFile, NodeBottomSheetAction.DELETE))

            with(expectMostRecentItem()) {
                assertTrue(this is ShowDeleteConfirmation)
                assertEquals(testFile, (this as ShowDeleteConfirmation).node)
            }
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
    fun `given view model when search query is updated then new load request is sent with updated search text`() =
        runTest(dispatcher) {

            val (arrangement, viewModel) = Arrangement()
                .withLoadSuccess()
                .arrange()

            viewModel.nodesFlow.test {
                viewModel.onSearchQueryUpdated("test")

                advanceTimeBy(1000)

                coVerify(exactly = 1) {
                    arrangement.getCellFilesPagedUseCase(null, "test")
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    private class Arrangement(conversationId: String? = null) {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getCellFilesPagedUseCase: GetPaginatedFilesFlowUseCase

        @MockK
        lateinit var getAllTagsUseCase: GetAllTagsUseCase

        @MockK
        lateinit var deleteCellAssetUseCase: DeleteCellAssetUseCase

        @MockK
        lateinit var downloadCellFileUseCase: DownloadCellFileUseCase

        @MockK
        lateinit var restoreNodeFromRecycleBinUseCase: RestoreNodeFromRecycleBinUseCase

        @MockK
        lateinit var fileHelper: FileHelper

        @MockK
        lateinit var kaliumFileSystem: KaliumFileSystem

        @MockK
        lateinit var context: Context

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<CellFilesNavArgs>() } returns CellFilesNavArgs(
                conversationId = conversationId
            )

            every { kaliumFileSystem.providePersistentAssetPath(any()) } returns localFilePath

            every { kaliumFileSystem.exists(any()) } returns false

            coEvery { getAllTagsUseCase.invoke() } returns emptySet<String>().right()
        }

        fun withLoadSuccess() = apply {
            coEvery { getCellFilesPagedUseCase(any(), any()) } returns flowOf(
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

        fun withDownloadFailure() = apply {
            coEvery { downloadCellFileUseCase(any(), any(), any(), any(), any()) } returns
                    CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun withDeleteSuccess() = apply {
            coEvery { deleteCellAssetUseCase(any(), any()) } returns Unit.right()
        }

        fun arrange(): Pair<Arrangement, CellViewModel> {
            return this to CellViewModel(
                savedStateHandle = savedStateHandle,
                getCellFilesPaged = getCellFilesPagedUseCase,
                deleteCellAsset = deleteCellAssetUseCase,
                getAllTagsUseCase = getAllTagsUseCase,
                restoreNodeFromRecycleBinUseCase = restoreNodeFromRecycleBinUseCase,
                download = downloadCellFileUseCase,
                fileHelper = fileHelper,
                kaliumFileSystem = kaliumFileSystem,
                context = context
            )
        }
    }
}
