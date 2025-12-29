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
package com.wire.android.feature.cells.ui.create.file

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.cells.ui.common.FileNameError
import com.wire.kalium.cells.domain.usecase.create.CreateDocumentFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreatePresentationFileUseCase
import com.wire.kalium.cells.domain.usecase.create.CreateSpreadsheetFileUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
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

class CreateFileViewModelTest {

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
    fun `given empty file name, when name changes, then save is disabled and no error`() = runTest(StandardTestDispatcher()) {
        // Given
        val (_, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.DOCUMENT)
            .arrange()

        // When
        viewModel.fileNameTextFieldState.edit { replace(0, length, "") }
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(null, viewModel.viewState.error)
    }

    @Test
    fun `given valid file name, when name changes, then save is enabled and no error`() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.DOCUMENT)
            .arrange()

        // When
        viewModel.fileNameTextFieldState.edit { replace(0, length, "Valid Name") }
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.viewState.saveEnabled)
        assertEquals(null, viewModel.viewState.error)
    }

    @Test
    fun `given invalid file name, when name changes, then save is disabled and error is shown`() = runTest {
        // Given
        val (_, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.DOCUMENT)
            .arrange()

        // When
        viewModel.fileNameTextFieldState.edit { replace(0, length, "Invalid/Name") }
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.viewState.saveEnabled)
        assertEquals(FileNameError.InvalidName, viewModel.viewState.error)
    }

    @Test
    fun `given document file type, when createFile is called, then document use case is invoked`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.DOCUMENT)
            .withCreateDocumentFileUseCaseReturning(Either.Right(Unit))
            .arrange()
        val fileName = "NewDoc"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createDocumentFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Success, viewModel.actions.first())
    }

    @Test
    fun `given failure from createDocumentUseCase, when createFile is called, then failure action is sent`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.DOCUMENT)
            .withCreateDocumentFileUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()
        val fileName = "NewDoc"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createDocumentFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Failure, viewModel.actions.first())
    }

    @Test
    fun `given spreadsheet file type, when createFile is called, then spreadsheet use case is invoked`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.SPREADSHEET)
            .withCreateSpreadsheetFileUseCaseReturning(Either.Right(Unit))
            .arrange()
        val fileName = "NewSheet"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createSpreadsheetFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Success, viewModel.actions.first())
    }

    @Test
    fun `given failure from createSpreadSheetUseCase, when createFile is called, then failure action is sent`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.SPREADSHEET)
            .withCreateSpreadsheetFileUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()
        val fileName = "NewSheet"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createSpreadsheetFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Failure, viewModel.actions.first())
    }

    @Test
    fun `given presentation file type, when createFile is called, then presentation use case is invoked`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.PRESENTATION)
            .withCreatePresentationFileUseCaseReturning(Either.Right(Unit))
            .arrange()
        val fileName = "NewSlides"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createPresentationFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Success, viewModel.actions.first())
    }

    @Test
    fun `given failure from createPresentationUseCase, when createFile is called, then failure action is sent`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withFileTypeReturning(FileType.PRESENTATION)
            .withCreatePresentationFileUseCaseReturning(Either.Left(CoreFailure.InvalidEventSenderID))
            .arrange()
        val fileName = "NewSlides"

        // When
        viewModel.createFile(fileName)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { arrangement.createPresentationFileUseCase(any()) }
        assertEquals(CreateFileViewModelAction.Failure, viewModel.actions.first())
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var createPresentationFileUseCase: CreatePresentationFileUseCase

        @MockK
        lateinit var createDocumentFileUseCase: CreateDocumentFileUseCase

        @MockK
        lateinit var createSpreadsheetFileUseCase: CreateSpreadsheetFileUseCase

        private val testUuid = "test-uuid"

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>("uuid") } returns testUuid
        }

        private val viewModel by lazy {
            CreateFileViewModel(
                savedStateHandle = savedStateHandle,
                createPresentationFileUseCase = createPresentationFileUseCase,
                createDocumentFileUseCase = createDocumentFileUseCase,
                createSpreadsheetFileUseCase = createSpreadsheetFileUseCase,
            )
        }

        fun withFileTypeReturning(result: FileType) = apply {
            every { savedStateHandle.get<FileType>("fileType") } returns result
        }

        fun withCreateDocumentFileUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { createDocumentFileUseCase(any()) } returns result
        }

        fun withCreateSpreadsheetFileUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { createSpreadsheetFileUseCase(any()) } returns result
        }

        fun withCreatePresentationFileUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { createPresentationFileUseCase(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
