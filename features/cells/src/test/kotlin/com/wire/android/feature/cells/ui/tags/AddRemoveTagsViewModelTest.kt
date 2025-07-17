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
    fun `given a new valid tag, when addTag is called, then tag is added and suggestions updated`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .arrange()
        val newTag = "compose"

        viewModel.addTag(newTag)

        val addedTags = viewModel.addedTags.first()
        assertTrue(addedTags.contains(newTag))

        val suggestedTags = viewModel.suggestedTags.first()
        assertFalse(suggestedTags.contains(newTag))

        assertEquals("", viewModel.tagsTextState.text)
    }

    @Test
    fun `given a blank tag, when addTag is called, then tag is not added`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .arrange()
        val blankTag = "   "

        viewModel.addTag(blankTag)

        val addedTags = viewModel.addedTags.first()
        assertTrue(addedTags.isEmpty())
    }

    @Test
    fun `given a duplicate tag, when addTag is called, then tag is not added again`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .arrange()

        val tag = "compose"
        viewModel.addTag(tag)

        viewModel.addTag(tag)

        val addedTags = viewModel.addedTags.first()
        assertEquals(1, addedTags.count { it == tag })
    }

    @Test
    fun `given a tag in suggestions, when addTag is called, then tag is removed from suggestions`() = runTest {
        val tagInSuggestions = "kotlin"

        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf(tagInSuggestions)))
            .arrange()

        advanceUntilIdle()
        assertTrue(viewModel.suggestedTags.first().contains(tagInSuggestions))

        viewModel.addTag(tagInSuggestions)

        val suggestions = viewModel.suggestedTags.first()
        assertFalse(suggestions.contains(tagInSuggestions))
    }

    @Test
    fun `given a tag in addedTags, when removeTag is called, then tag is removed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .arrange()
        val tag = "compose"
        viewModel.addTag(tag)
        assertTrue(viewModel.addedTags.first().contains(tag))

        viewModel.removeTag(tag)

        val addedTags = viewModel.addedTags.first()
        assertFalse(addedTags.contains(tag))
    }

    @Test
    fun `given a tag not in addedTags, when removeTag is called, then addedTags remains unchanged`() = runTest {
        val (_, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .arrange()
        val existingTag = "compose"
        val nonExistentTag = "android"
        viewModel.addTag(existingTag)
        val initialTags = viewModel.addedTags.first()

        // When
        viewModel.removeTag(nonExistentTag)

        // Then
        val currentTags = viewModel.addedTags.first()
        assertEquals(initialTags, currentTags)
    }

    @Test
    fun `given empty tag list, when updateTags is called, then removeNodeTagsUseCase is invoked and success is sent`() = runTest {

        val (arrangement, viewModel) = Arrangement()
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
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
        val tags = listOf("compose", "kotlin")

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
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
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
            .withGetAllTagsUseCaseReturning(Either.Right(listOf()))
            .withUpdateNodeTagsUseCaseReturning(Either.Left(CoreFailure.MissingClientRegistration))
            .arrange()

        viewModel.addTag("android")
        viewModel.actions.test {
            viewModel.updateTags()
            advanceUntilIdle()

            assertEquals(AddRemoveTagsViewModelAction.Failure, awaitItem())
        }
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

        private val viewModel by lazy {
            AddRemoveTagsViewModel(
                savedStateHandle = savedStateHandle,
                getAllTagsUseCase = getAllTagsUseCase,
                updateNodeTagsUseCase = updateNodeTagsUseCase,
                removeNodeTagsUseCase = removeNodeTagsUseCase,
            )
        }

        fun withGetAllTagsUseCaseReturning(result: Either<CoreFailure, List<String>>) = apply {
            coEvery { getAllTagsUseCase() } returns result
        }

        fun withUpdateNodeTagsUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { updateNodeTagsUseCase(any(), any()) } returns result
        }

        fun withRemoveNodeTagsUseCaseReturning(result: Either<CoreFailure, Unit>) = apply {
            coEvery { removeNodeTagsUseCase(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
