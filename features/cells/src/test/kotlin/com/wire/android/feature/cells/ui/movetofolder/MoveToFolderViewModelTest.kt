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
package com.wire.android.feature.cells.ui.movetofolder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.navArgs
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodeUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NavigationTestExtension::class)
class MoveToFolderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given getCellFilesUseCase success, when view model created, then emit nodes and update state to Success`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(
                Either.Right(
                    listOf(
                        Node.Folder(
                            name = "folderName",
                            userName = "userName",
                            conversationName = "conversationName",
                            uuid = "uuid",
                            modifiedTime = 0L,
                            remotePath = "",
                            size = 12434,
                        )
                    )
                )
            )
            .arrange()

        viewModel.state.test {
            skipItems(1)
            val state = awaitItem()
            assertEquals(MoveToFolderScreenState.SUCCESS, state.screenState)
            assertTrue(state.folders.isNotEmpty())
        }
    }

    @Test
    fun `given getCellFilesUseCase failure, when view model created, then update state to Failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.state.test {
            skipItems(1)
            assertEquals(MoveToFolderScreenState.ERROR, awaitItem().screenState)
        }
    }

    @Test
    fun `given moveNodeUseCase success, when moveToFolder is called, then send success action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodeUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            with(awaitItem()) {
                assertTrue(this is MoveToFolderViewAction.Success)
            }
        }
    }

    @Test
    fun `given moveNodeUseCase failure, when moveToFolder is called, then send failure action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodeUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            with(awaitItem()) {
                assertTrue(this is MoveToFolderViewAction.Failure)
            }
        }
    }

    @Test
    fun `when onCreateFolderClick is called, then send open create folder action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .arrange()

        viewModel.actions.test {
            viewModel.onCreateFolderClick()
            with(awaitItem()) {
                assertTrue(this is MoveToFolderViewAction.OpenCreateFolderScreen)
                assertEquals(CURRENT_PATH, (this as MoveToFolderViewAction.OpenCreateFolderScreen).currentPath)
            }
        }
    }

    @Test
    fun `when onBreadcrumbClick is called, then send navigate to breadcrumb action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .arrange()

        viewModel.actions.test {
            viewModel.onBreadcrumbClick(2)
            with(awaitItem()) {
                assertTrue(this is MoveToFolderViewAction.NavigateToBreadcrumb)
                assertEquals(0, (this as MoveToFolderViewAction.NavigateToBreadcrumb).steps)
            }
        }
    }

    @Test
    fun `when onFolderClick is called, then send navigate to folder action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .arrange()
        val folder = Node.Folder(
            name = "folderName",
            userName = "userName",
            conversationName = "conversationName",
            uuid = "uuid",
            modifiedTime = 0L,
            remotePath = "",
            size = 12434,
        ).toUiModel()

        viewModel.actions.test {
            viewModel.onFolderClick(folder)
            with(awaitItem()) {
                assertTrue(this is MoveToFolderViewAction.OpenFolder)
                assertEquals(
                    "$CURRENT_PATH/${folder.name}",
                    (this as MoveToFolderViewAction.OpenFolder).path
                )
                assertEquals(
                    NODE_TO_MOVE_PATH,
                    this.nodePath
                )
            }
        }
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getFoldersUseCase: GetFoldersUseCase

        @MockK
        lateinit var moveNodeUseCase: MoveNodeUseCase

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<MoveToFolderNavArgs>() } returns MoveToFolderNavArgs(
                currentPath = CURRENT_PATH,
                nodeToMovePath = NODE_TO_MOVE_PATH,
                uuid = UUID,
                breadcrumbs = BREADCRUMBS
            )
        }

        private val viewModel by lazy {
            MoveToFolderViewModel(
                savedStateHandle = savedStateHandle,
                getFoldersUseCase = getFoldersUseCase,
                moveNodeUseCase = moveNodeUseCase,
            )
        }

        fun withGetFoldersUseCaseReturning(result: Either<CoreFailure, List<Node.Folder>>) = apply {
            coEvery { getFoldersUseCase(any()) } returns result
        }

        fun withMoveNodeUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { moveNodeUseCase(any(), any(), any()) } returns result
        }

        fun arrange() = this to viewModel
    }

    companion object {
        const val CURRENT_PATH = "currentPath"
        const val NODE_TO_MOVE_PATH = "nodeToMovePath"
        const val UUID = "uuid"
        val BREADCRUMBS = arrayOf("Folder1", "Folder2", "Folder3")
    }
}
