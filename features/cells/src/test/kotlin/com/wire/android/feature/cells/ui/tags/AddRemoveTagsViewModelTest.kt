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
package com.wire.android.feature.cells.ui.tags

import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderViewModelTest.Companion.UUID
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddRemoveTagsViewModelTest {

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
    fun `given a new valid tag, when addTag is called, then tag is added`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .arrange()
        val newTag = "compose"

        viewModel.state.test {
            skipItems(1)
            viewModel.addTag(newTag)
            val addedTags = awaitItem().addedTags
            assertTrue(addedTags.contains(newTag))
        }
    }

    @Test
    fun `given a blank tag, when addTag is called, then tag is not added`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .arrange()
        val blankTag = "   "

        viewModel.state.test {
            viewModel.addTag(blankTag)
            val addedTags = awaitItem().addedTags
            assertTrue(addedTags.isEmpty())
        }
    }

    @Test
    fun `given a duplicate tag, when addTag is called, then tag is not added again`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .arrange()

        val tag = "compose"

        viewModel.state.test {
            skipItems(1)
            viewModel.addTag(tag)
            viewModel.addTag(tag)
            val addedTags = awaitItem().addedTags
            assertEquals(1, addedTags.count { it == tag })
        }
    }

    @Test
    fun `given a tag in suggestions, when addTag is called, then tag is removed from suggestions`() = runTest {
        val tagInSuggestions = "kotlin"

        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf(tagInSuggestions)))
            .arrange()

        advanceUntilIdle()

        viewModel.state.test {
            assertTrue(awaitItem().suggestedTags.contains(tagInSuggestions))
            viewModel.addTag(tagInSuggestions)
            assertFalse(awaitItem().suggestedTags.contains(tagInSuggestions))
        }
    }

    @Test
    fun `given a tag in addedTags, when removeTag is called, then tag is removed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .arrange()
        val tag = "compose"

        viewModel.state.test {

            skipItems(1)

            viewModel.addTag(tag)
            assertTrue(awaitItem().addedTags.contains(tag))

            viewModel.removeTag(tag)

            val addedTags = awaitItem().addedTags
            assertFalse(addedTags.contains(tag))
        }
    }

    @Test
    fun `given a tag not in addedTags, when removeTag is called, then addedTags remains unchanged`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .arrange()

        val existingTag = "compose"
        val nonExistentTag = "android"

        viewModel.addTag(existingTag)

        viewModel.state.test {

            skipItems(1)

            // When
            viewModel.removeTag(nonExistentTag)

            // Then
            expectNoEvents()
        }
    }

    @Test
    fun `given empty tag list, when updateTags is called, then removeNodeTagsUseCase is invoked and success is sent`() = runTest {

        val (arrangement, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .withRemoveNodeTagsUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            viewModel.updateTags()
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.removeNodeTagsUseCase(any()) }
            coVerify(exactly = 0) { arrangement.updateNodeTagsUseCase(any(), any()) }
            assertEquals(AddRemoveTagsViewModelAction.Success, awaitItem())
        }
    }

    @Test
    fun `given non-empty tag list, when updateTags is called, then updateNodeTagsUseCase is invoked and success is sent`() = runTest {
        val tags = setOf("compose", "kotlin")

        val (arrangement, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(tags))
            .withUpdateNodeTagsUseCaseReturning(Either.Right(Unit))
            .arrange()

        viewModel.actions.test {
            tags.forEach { viewModel.addTag(it) }

            viewModel.updateTags()

            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.removeNodeTagsUseCase(any()) }
            coVerify(exactly = 1) { arrangement.updateNodeTagsUseCase(any(), any()) }
            assertEquals(AddRemoveTagsViewModelAction.Success, awaitItem())
        }
    }

    @Test
    fun `given removeNodeTagsUseCase fails, when updateTags is called, then failure is sent`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .withRemoveNodeTagsUseCaseReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .arrange()

        viewModel.actions.test {
            viewModel.updateTags()
            advanceUntilIdle()

            assertEquals(AddRemoveTagsViewModelAction.Failure, awaitItem())
        }
    }

    @Test
    fun `given updateNodeTagsUseCase fails, when updateTags is called, then failure is sent`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(setOf()))
            .withUpdateNodeTagsUseCaseReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .arrange()

        viewModel.addTag("android")
        viewModel.actions.test {
            viewModel.updateTags()
            advanceUntilIdle()

            assertEquals(AddRemoveTagsViewModelAction.Failure, awaitItem())
        }
    }

    @Test
    fun `given valid input when isValidTag called then returns true`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()
        viewModel.tagsTextState.setTextAndSelectAll("ValidTag123")

        // When
        val result = viewModel.isValidTag()

        // Then
        assertTrue(result)
    }

    @Test
    fun `given multiple invalid inputs when isValidTag called then returns false`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()

        val invalidInputs = listOf(
            "Invalid,Tag",
            "Invalid;Tag",
            "Invalid/Tag",
            "Invalid\\Tag",
            "Invalid\"Tag",
            "Invalid'Tag",
            "Invalid<Tag",
            "Invalid>Tag",
            "Inva<lid>/Tag\\"
        )

        // When & Then
        invalidInputs.forEach { input ->
            viewModel.tagsTextState.setTextAndSelectAll(input)
            val result = viewModel.isValidTag()
            assertFalse(result)
        }
    }

    @Test
    fun `given input outside length range when isValidTag called then returns false`() {
        // Given
        val (_, viewModel) = Arrangement().arrange()
        val tooShort = "" // length 0
        val tooLong = "A".repeat(31) // length 31

        // When
        val resultTooShort = viewModel.isValidTag()
        viewModel.tagsTextState.setTextAndSelectAll(tooShort)
        assertFalse(resultTooShort)

        val resultTooLong = viewModel.isValidTag()
        viewModel.tagsTextState.setTextAndSelectAll(tooLong)
        assertFalse(resultTooLong)
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getAllTagsUseCase: GetAllTagsUseCase

        @MockK
        lateinit var updateNodeTagsUseCase: UpdateNodeTagsUseCase

        @MockK
        lateinit var removeNodeTagsUseCase: RemoveNodeTagsUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>("uuid") } returns UUID
            every { savedStateHandle.get<ArrayList<String>>("tags") } returns ArrayList()
        }

        fun withGetAllTagsUseCaseReturning(result: Either<CoreFailure, Set<String>>) = apply {
            coEvery { getAllTagsUseCase() } returns result
        }

        fun withUpdateNodeTagsUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { updateNodeTagsUseCase(any(), any()) } returns result
        }

        fun withRemoveNodeTagsUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { removeNodeTagsUseCase(any()) } returns result
        }

        fun arrange(): Pair<Arrangement, AddRemoveTagsViewModel> {
            // Create a new ViewModel instance every time arrange() is called.
            // This prevents state from leaking between tests.
            val viewModel = AddRemoveTagsViewModel(
                savedStateHandle = savedStateHandle,
                getAllTagsUseCase = getAllTagsUseCase,
                updateNodeTagsUseCase = updateNodeTagsUseCase,
                removeNodeTagsUseCase = removeNodeTagsUseCase,
            )
            return this to viewModel
        }
    }
}
