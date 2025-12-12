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
import com.wire.android.feature.cells.ui.common.FileNameError
import com.wire.android.feature.cells.ui.navArgs
import com.wire.kalium.cells.domain.usecase.RenameNodeFailure
import com.wire.kalium.cells.domain.usecase.RenameNodeUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.left
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
import org.junit.jupiter.api.Assertions.assertFalse
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
                assertEquals(false, viewModel.viewState.loading)
                assertTrue(this is RenameNodeViewModelAction.Success)
                coVerify(exactly = 1) { arrangement.renameNodeUseCase(eq(UUID), eq(CURRENT_PATH), eq("newFileName.txt")) }
            }
        }
    }

    @Test
    fun `given renameNodeUseCase failure, when rename is called, then send failure action`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRenameNodeUseCaseReturning(RenameNodeFailure.Other(CoreFailure.InvalidEventSenderID).left())
            .arrange()

        viewModel.renameNode("")

        advanceUntilIdle()

        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertTrue(this is RenameNodeViewModelAction.Failure)
                coVerify(exactly = 1) { arrangement.renameNodeUseCase(any(), any(), any()) }
            }
        }
    }

    @Test
    fun `given invalid name with slash, when text is emitted, then InvalidNameError is set`() = runTest {
        val invalidName = "invalid/name"
        val (_, viewModel) = Arrangement()
            .withNodeNameReturning(invalidName)
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(
            FileNameError.InvalidName,
            viewModel.viewState.error
        )
    }

    @Test
    fun `given invalid name starting with dot, when text is emitted, then InvalidNameError is set`() = runTest {
        val invalidName = "."
        val (_, viewModel) = Arrangement()
            .withNodeNameReturning(invalidName)
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(
            FileNameError.InvalidName,
            viewModel.viewState.error
        )
    }

    @Test
    fun `given empty name, when text is emitted, then NameEmptyError is set`() = runTest {
        val emptyName = ""
        val (_, viewModel) = Arrangement()
            .withNodeNameReturning(emptyName)
            .arrange()

        advanceUntilIdle()
        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(
            FileNameError.NameEmpty,
            viewModel.viewState.error
        )
    }

    @Test
    fun `given long name, when text is emitted, then NameExceedLimitError is set`() = runTest {
        val longName = "Long name that exceeds the limit of sixty four characters" +
                " which is the maximum allowed for a file name in this application"
        val (_, viewModel) = Arrangement()
            .withNodeNameReturning(longName)
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(
            FileNameError.NameExceedLimit,
            viewModel.viewState.error
        )
    }

    @Test
    fun `given name not changed, when text is emitted, then save is disabled`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(
            null,
            viewModel.viewState.error
        )
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
            every { savedStateHandle.get<Boolean>("isFolder") } returns false
            every { savedStateHandle.get<String>("nodeName") } returns NODE_NAME
        }

        private val viewModel by lazy {
            RenameNodeViewModel(
                savedStateHandle = savedStateHandle,
                renameNodeUseCase = renameNodeUseCase,
            )
        }

        fun withRenameNodeUseCaseReturning(result: Either<RenameNodeFailure, Unit>) = apply {
            coEvery { renameNodeUseCase(any(), any(), any()) } returns result
        }
        fun withNodeNameReturning(name: String) = apply {
            every { savedStateHandle.get<String>("nodeName") } returns name
        }

        fun arrange() = this to viewModel
    }

    companion object {
        const val CURRENT_PATH = "currentPath"
        const val UUID = "uuid"
        const val NODE_NAME = "nodeName.txt"
    }
}
