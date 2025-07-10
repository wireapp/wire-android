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
package com.wire.android.feature.cells.ui.rename

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.model.DisplayNameState
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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

class RenameNodeViewModelTest {

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
    fun `given renameNodeUseCase success, when rename is called, then send success action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRenameNodeUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.renameNode("newFileName")

        advanceUntilIdle()
        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertEquals(false, viewModel.displayNameState.loading)
                assertEquals(DisplayNameState.Completed.Success, viewModel.displayNameState.completed)
                assertTrue(this is RenameNodeViewModelAction.Success)
                coVerify(exactly = 1) { arrangement.renameNodeUseCase(eq(UUID), eq(CURRENT_PATH), eq("newFileName.txt")) }
            }
        }
    }

    @Test
    fun `given renameNodeUseCase failure, when rename is called, then send failure action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRenameNodeUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()

        viewModel.renameNode("")

        advanceUntilIdle()
        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertEquals(false, viewModel.displayNameState.loading)
                assertEquals(DisplayNameState.Completed.Failure, viewModel.displayNameState.completed)
                assertTrue(this is RenameNodeViewModelAction.Failure)
                coVerify(exactly = 1) { arrangement.renameNodeUseCase(any(), any(), any()) }
            }
        }
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var renameNodeUseCase: RenameNodeUseCase

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<RenameNodeNavArgs>() } returns RenameNodeNavArgs(
                uuid = UUID,
                currentPath = CURRENT_PATH,
                nodeName = NODE_NAME,
                isFolder = true
            )
            every { savedStateHandle.get<String>("uuid") } returns UUID
            every { savedStateHandle.get<String>("currentPath") } returns CURRENT_PATH
            every { savedStateHandle.get<Boolean>("isFolder") } returns true
            every { savedStateHandle.get<String>("nodeName") } returns NODE_NAME
        }

        private val viewModel by lazy {
            RenameNodeViewModel(
                savedStateHandle = savedStateHandle,
                renameNodeUseCase = renameNodeUseCase,
            )
        }

        fun withRenameNodeUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { renameNodeUseCase(any(), any(), any()) } returns result
        }

        fun arrange() = this to viewModel
    }

    companion object {
        const val CURRENT_PATH = "currentPath"
        const val UUID = "uuid"
        const val NODE_NAME = "nodeName.txt"
    }
}
