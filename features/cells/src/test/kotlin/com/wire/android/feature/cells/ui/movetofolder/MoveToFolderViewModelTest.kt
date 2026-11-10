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

import app.cash.turbine.test
import com.wire.android.feature.cells.ui.CellNodeChangeEvents
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.model.NodeLocator
import com.wire.kalium.cells.domain.usecase.GetFoldersUseCase
import com.wire.kalium.cells.domain.usecase.MoveNodesUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
                            uuid = "folderUuid",
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
    fun `given moveNodesUseCase success, when moveToFolder is called, then send success action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            assertEquals(MoveToFolderViewAction.Success(movedCount = 1, destinationFolderName = "Folder3"), awaitItem())
        }
    }

    @Test
    fun `given moveNodesUseCase failure, when moveToFolder is called, then expose the move failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.onMoveToFolderClick()
        advanceUntilIdle()

        assertEquals(MoveFailure(totalCount = 1, isNoNetworkConnection = false), viewModel.state.value.moveFailure)
        assertFalse(viewModel.isMoving())
    }

    @Test
    fun `given no network connection, when moveToFolder fails, then expose it as a no network move failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Left(NetworkFailure.NoNetworkConnection(null)))
            .arrange()

        viewModel.onMoveToFolderClick()
        advanceUntilIdle()

        assertEquals(MoveFailure(totalCount = 1, isNoNetworkConnection = true), viewModel.state.value.moveFailure)
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
                    listOf(NodeToMove(UUID, NODE_TO_MOVE_PATH)),
                    this.nodesToMove
                )
            }
        }
    }

    @Test
    fun `given several nodes, when moved, then move them all in a single request and send success`() = runTest {
        val nodes = bulkNodes()
        val (arrangement, viewModel) = Arrangement(nodes)
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            assertEquals(MoveToFolderViewAction.Success(movedCount = 3, destinationFolderName = "Folder3"), awaitItem())
        }
        coVerify(exactly = 1) {
            arrangement.moveNodesUseCase(nodes.map { NodeLocator(uuid = it.uuid, path = it.path) }, CURRENT_PATH)
        }
    }

    @Test
    fun `given several nodes, when moved, then report the source and destination folders as changed`() = runTest {
        val (arrangement, viewModel) = Arrangement(bulkNodes())
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Right(Unit))
            .arrange()

        arrangement.nodeChangeEvents.changedFolderPaths.test {
            viewModel.onMoveToFolderClick()
            assertEquals(setOf("$CURRENT_PATH/parent", CURRENT_PATH), awaitItem())
        }
    }

    @Test
    fun `given the conversation root as destination, when moved, then send success without a folder name`() = runTest {
        val (_, viewModel) = Arrangement(breadcrumbs = emptyArray())
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            assertEquals(MoveToFolderViewAction.Success(movedCount = 1, destinationFolderName = null), awaitItem())
        }
    }

    @Test
    fun `given a move in progress, when clicking move or navigating, then ignore it until the move completes`() = runTest {
        val moveResult = CompletableDeferred<Either<CoreFailure, Unit>>()
        val (arrangement, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .arrange()
        coEvery { arrangement.moveNodesUseCase(any(), any()) } coAnswers { moveResult.await() }
        advanceUntilIdle()

        viewModel.actions.test {
            viewModel.onMoveToFolderClick()
            advanceUntilIdle()
            assertTrue(viewModel.isMoving())

            viewModel.onMoveToFolderClick()
            viewModel.onFolderClick(folder("folderUuid").toUiModel())
            viewModel.onBreadcrumbClick(0)
            viewModel.onCreateFolderClick()
            advanceUntilIdle()
            expectNoEvents()

            moveResult.complete(Either.Right(Unit))
            assertEquals(MoveToFolderViewAction.Success(movedCount = 1, destinationFolderName = "Folder3"), awaitItem())
        }
        assertFalse(viewModel.isMoving())
        coVerify(exactly = 1) { arrangement.moveNodesUseCase(any(), any()) }
    }

    @Test
    fun `given several nodes, when the move fails, then expose the failure and still report the folders as changed`() = runTest {
        val (arrangement, viewModel) = Arrangement(bulkNodes())
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodesUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        arrangement.nodeChangeEvents.changedFolderPaths.test {
            viewModel.onMoveToFolderClick()
            assertEquals(setOf("$CURRENT_PATH/parent", CURRENT_PATH), awaitItem())
        }
        advanceUntilIdle()
        assertEquals(MoveFailure(totalCount = 3, isNoNetworkConnection = false), viewModel.state.value.moveFailure)
    }

    @Test
    fun `given a folder being moved, when folders are loaded, then it is not offered as a destination`() = runTest {
        val movedFolder = NodeToMove(uuid = "movedFolderUuid", path = "$CURRENT_PATH/parent/moved")
        val (_, viewModel) = Arrangement(listOf(movedFolder))
            .withGetFoldersUseCaseReturning(
                Either.Right(listOf(folder(uuid = movedFolder.uuid), folder(uuid = "otherFolderUuid")))
            )
            .arrange()

        viewModel.state.test {
            skipItems(1)
            assertEquals(listOf("otherFolderUuid"), awaitItem().folders.map { it.uuid })
        }
    }

    private fun bulkNodes() = listOf(
        NodeToMove(uuid = "uuid1", path = "$CURRENT_PATH/parent/file1.txt"),
        NodeToMove(uuid = "uuid2", path = "$CURRENT_PATH/parent/folder"),
        NodeToMove(uuid = "uuid3", path = "$CURRENT_PATH/parent/file2.pdf"),
    )

    private fun folder(uuid: String) = Node.Folder(
        name = uuid,
        userName = "userName",
        conversationName = "conversationName",
        uuid = uuid,
        modifiedTime = 0L,
        remotePath = "",
        size = 0,
    )

    private class Arrangement(
        private val nodesToMove: List<NodeToMove> = listOf(NodeToMove(UUID, NODE_TO_MOVE_PATH)),
        private val breadcrumbs: Array<String> = BREADCRUMBS,
    ) {

        @MockK
        lateinit var getFoldersUseCase: GetFoldersUseCase

        @MockK
        lateinit var moveNodesUseCase: MoveNodesUseCase

        val nodeChangeEvents = CellNodeChangeEvents()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        private val viewModel by lazy {
            MoveToFolderViewModel(
                navArgs = MoveToFolderNavArgs(CURRENT_PATH, nodesToMove, breadcrumbs),
                getFoldersUseCase = getFoldersUseCase,
                moveNodesUseCase = moveNodesUseCase,
                nodeChangeEvents = nodeChangeEvents,
            )
        }

        fun withGetFoldersUseCaseReturning(result: Either<CoreFailure, List<Node.Folder>>) = apply {
            coEvery { getFoldersUseCase(any()) } returns result
        }

        fun withMoveNodesUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { moveNodesUseCase(any(), any()) } returns result
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
