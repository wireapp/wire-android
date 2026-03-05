/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetCellGroupConversationsUseCase
import com.wire.kalium.cells.domain.usecase.GetConversationsUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class SearchScreenViewModelTest {

    private companion object {
        const val CONVERSATION_ID = "conversationId123"
        val mockTags = setOf("tag1", "tag2", "tag3")
    }

    private val dispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase

    @MockK
    private lateinit var getCellFilesPaged: GetPaginatedFilesFlowUseCase

    @MockK
    private lateinit var getOwners: GetOwnersUseCase

    @MockK
    private lateinit var getCellGroupConversations: GetCellGroupConversationsUseCase

    private lateinit var savedStateHandle: SavedStateHandle

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)

        val navArgsMap = mapOf<String, Any?>(
            "conversationId" to CONVERSATION_ID,
            "screenType" to DriveSearchScreenType.SHARED_DRIVE
        )

        savedStateHandle = mockk(relaxed = true)

        every { savedStateHandle.get<Any?>(any()) } answers {
            val key = firstArg<String>()
            navArgsMap[key]
        }

        MockKAnnotations.init(this)

        coEvery { getAllTagsUseCase() } returns Either.Right(mockTags)
        coEvery { getCellFilesPaged(any(), any(), any(), any()) } returns flowOf(PagingData.empty())
        coEvery { getOwners(any()) } returns GetOwnersUseCaseResult.Failure(CoreFailure.InvalidEventSenderID)
        coEvery { getCellGroupConversations() } returns GetConversationsUseCaseResult.Success(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given initial state, when ViewModel is created, then tags are loaded`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.availableTags.size)
    }

    @Test
    fun `given ViewModel, when onSearchQueryChanged is called, then query is updated`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("test query")
    }

    @Test
    fun `given tags in state, when onSaveTags is called, then tags are marked as selected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val tagsToSelect = listOf(
            FilterTagUi(id = "tag1", name = "tag1", selected = true),
            FilterTagUi(id = "tag2", name = "tag2", selected = true)
        )

        viewModel.onSaveTags(tagsToSelect)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.tagsCount)
    }

    @Test
    fun `given selected tags, when onRemoveAllTags is called, then all tags are deselected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveTags(listOf(FilterTagUi(id = "tag1", name = "tag1", selected = true)))
        advanceUntilIdle()

        viewModel.onRemoveAllTags()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.tagsCount)
        assertFalse(viewModel.uiState.value.availableTags.any { it.selected })
    }

    @Test
    fun `given state, when onSharedByMeClicked is called, then filesWithPublicLink is toggled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filesWithPublicLink)

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.filesWithPublicLink)
    }

    @Test
    fun `given filters applied, when onRemoveAllFilters is called, then all filters are cleared`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveTags(listOf(FilterTagUi(id = "tag1", name = "tag1", selected = true)))
        advanceUntilIdle()

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()

        viewModel.onRemoveAllFilters()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.tagsCount)
        assertFalse(viewModel.uiState.value.filesWithPublicLink)
    }

    @Test
    fun `given state, when setSortBy is called with new sort criteria, then sorting is updated`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setSortBy(SortBy.Name)
        advanceUntilIdle()

        assertEquals(SortBy.Name, viewModel.uiState.value.sortingCriteria.by)
    }

    @Test
    fun `given state, when setSortBy is called with the same sort criteria, then state is not changed`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialSortingCriteria = viewModel.uiState.value.sortingCriteria

        viewModel.setSortBy(SortBy.Modified)
        advanceUntilIdle()

        assertEquals(initialSortingCriteria, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given state, when setSorting is called, then sorting criteria is updated`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val newCriteria = SortingCriteria.Name.ZtoA

        viewModel.setSorting(newCriteria)
        advanceUntilIdle()

        assertEquals(newCriteria, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given state, when onRemoveTypeFilter is called, then all types are deselected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRemoveTypeFilter()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.typeCount)
    }

    private fun createViewModel(): SearchScreenViewModel {
        return SearchScreenViewModel(
            savedStateHandle = savedStateHandle,
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getCellGroupConversations = getCellGroupConversations,
        )
    }
}
