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
package com.wire.android.feature.cells.ui.create.folder

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.cells.ui.common.FileNameError
import com.wire.kalium.cells.domain.usecase.create.CreateFolderUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFolderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given empty folder name, when name changes, then save is disabled and no error`() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .arrange()

        // When
        viewModel.folderNameTextFieldState.edit { replace(0, length, "") }
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(null, viewModel.viewState.error)
    }

    @Test
    fun `given valid folder name, when name changes, then save is enabled and no error`() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .arrange()

        // When
        viewModel.folderNameTextFieldState.edit { replace(0, length, "Valid Folder") }
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.viewState.saveEnabled)
        assertEquals(null, viewModel.viewState.error)
    }

    @Test
    fun `given invalid folder name, when name changes, then save is disabled and error is shown`() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .arrange()

        // When
        viewModel.folderNameTextFieldState.edit { replace(0, length, "Invalid/Folder") }
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(FileNameError.InvalidName, viewModel.viewState.error)
    }

    @Test
    fun `given valid folder name, when createFolder is called, then create folder use case is invoked`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withCreateFolderUseCaseReturning(Either.Right(Unit))
            .arrange()
        val folderName = "NewFolder"

        // When
        viewModel.createFolder(folderName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createFolderUseCase(any()) }
        assertEquals(CreateFolderViewModelAction.Success, viewModel.actions.first())
    }

    @Test
    fun `given failure from createFolderUseCase, when createFolder is called, then failure action is sent`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withCreateFolderUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()
        val folderName = "NewFolder"

        // When
        viewModel.createFolder(folderName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createFolderUseCase(any()) }
        assertEquals(CreateFolderViewModelAction.Failure, viewModel.actions.first())
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var createFolderUseCase: CreateFolderUseCase

        private val testUuid = "test-uuid"

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>("uuid") } returns testUuid
        }

        private val viewModel by lazy {
            CreateFolderViewModel(
                savedStateHandle = savedStateHandle,
                createFolderUseCase = createFolderUseCase,
            )
        }

        fun withCreateFolderUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { createFolderUseCase(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
