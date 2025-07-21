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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `given getCellFilesUseCase success, when loadFiles is called, then emit nodes and update state to Success`() = runTest {
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

        viewModel.loadFolders()

        advanceUntilIdle()
        assertEquals(MoveToFolderScreenState.SUCCESS, viewModel.state.value)
        assertTrue(viewModel.folders.value.isNotEmpty())
    }

    @Test
    fun `given getCellFilesUseCase failure, when loadFiles is called, then update state to Failure`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.loadFolders()

        advanceUntilIdle()
        assertEquals(MoveToFolderScreenState.ERROR, viewModel.state.value)
    }

    @Test
    fun `given moveNodeUseCase success, when moveHere is called, then send success action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodeUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.moveHere()

        advanceUntilIdle()
        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertTrue(this is MoveToFolderViewAction.Success)
            }
        }
    }

    @Test
    fun `given moveNodeUseCase failure, when moveHere is called, then send failure action`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetFoldersUseCaseReturning(Either.Right(listOf()))
            .withMoveNodeUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.moveHere()

        advanceUntilIdle()
        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertTrue(this is MoveToFolderViewAction.Failure)
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
            )
            every { savedStateHandle.get<String>("currentPath") } returns CURRENT_PATH
            every { savedStateHandle.get<String>("nodeToMovePath") } returns NODE_TO_MOVE_PATH
            every { savedStateHandle.get<String>("uuid") } returns UUID
            every { savedStateHandle.get<String>("screenName") } returns "screenName"
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
    }
}
