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
package com.wire.android.ui.home.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestMessage
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.SearchMessagesGloballyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class GlobalSearchViewModelTest {

    @Test
    fun givenBlankSearchQuery_whenQueryChanges_thenSearchIsNotTriggered() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(" ")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.results.isEmpty())
        coVerify(exactly = 0) {
            arrangement.searchMessagesGlobally(any(), any(), any())
        }
    }

    @Test
    fun givenSearchResults_whenQueryChanges_thenMessagesAreMappedToUiState() = runTest {
        val message = TestMessage.TEXT_MESSAGE.copy(
            id = "message-id",
            content = MessageContent.Text("The release moves to Friday"),
            senderUserName = "Alice"
        )
        val (arrangement, viewModel) = Arrangement()
            .withGlobalSearchResult(SearchMessagesGloballyUseCase.Result.Success(listOf(message)))
            .arrange()

        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd("release")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertEquals("release", state.searchQuery)
        assertEquals(1, state.results.size)
        assertEquals("message-id", state.results.first().messageId)
        assertEquals("Alice", state.results.first().senderName)
        assertEquals("GROUP Name", state.results.first().conversationName)
        assertEquals("The release moves to Friday", state.results.first().preview)
        coVerify {
            arrangement.searchMessagesGlobally("release", 100, 0)
        }
    }

    @Test
    fun givenSearchFailure_whenQueryChanges_thenErrorStateIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withGlobalSearchResult(SearchMessagesGloballyUseCase.Result.Failure(StorageFailure.DataNotFound))
            .arrange()

        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd("release")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasError)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    @Test
    fun givenDisabledFilter_whenFilterSelected_thenSelectedFilterDoesNotChange() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onFilterSelected(GlobalSearchFilter.Files)

        assertEquals(GlobalSearchFilter.All, viewModel.state.value.selectedFilter)
    }

    @Test
    fun givenConversationDetailsThrows_whenQueryChanges_thenDeletedAccountLabelIsUsed() = runTest {
        val message = TestMessage.TEXT_MESSAGE.copy(
            id = "message-id",
            content = MessageContent.Text("The release moves to Friday"),
            senderUserName = null,
            sender = null,
        )
        val (_, viewModel) = Arrangement()
            .withGlobalSearchResult(SearchMessagesGloballyUseCase.Result.Success(listOf(message)))
            .withConversationDetailsThrowing()
            .arrange()

        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd("release")
        advanceUntilIdle()

        val result = viewModel.state.value.results.first()
        assertEquals(DELETED_ACCOUNT_LABEL, result.senderName)
        assertEquals(DELETED_ACCOUNT_LABEL, result.conversationName)
    }

    private class Arrangement {
        @MockK
        lateinit var searchMessagesGlobally: SearchMessagesGloballyUseCase

        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        private val viewModel: GlobalSearchViewModel by lazy {
            GlobalSearchViewModel(
                searchMessagesGlobally = searchMessagesGlobally,
                observeConversationDetails = observeConversationDetails,
                dispatchers = TestDispatcherProvider(),
                uiTextResolver = object : UiTextResolver {
                    override fun resolve(text: UIText): String = DELETED_ACCOUNT_LABEL
                    override fun localeTag(): String = "en"
                }
            )
        }

        init {
            MockKAnnotations.init(this)
            coEvery {
                searchMessagesGlobally(any(), any(), any())
            } returns SearchMessagesGloballyUseCase.Result.Success(emptyList())
            coEvery {
                observeConversationDetails(any())
            } returns flowOf(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.GROUP))
        }

        fun withGlobalSearchResult(result: SearchMessagesGloballyUseCase.Result) = apply {
            coEvery {
                searchMessagesGlobally(any(), any(), any())
            } returns result
        }

        fun withConversationDetailsThrowing() = apply {
            coEvery {
                observeConversationDetails(any())
            } returns flow { throw IllegalArgumentException("Field otherUserID in OneOnOne null when unpacking db content") }
        }

        fun arrange() = this to viewModel
    }

    private companion object {
        const val DELETED_ACCOUNT_LABEL = "Deleted account"
    }
}
