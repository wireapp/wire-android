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

package com.wire.android.ui.home.conversationslist.search

import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestMessage
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.common.DEFAULT_SEARCH_QUERY_DEBOUNCE
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetUsersForMessageUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.feature.message.SearchMessagesSemanticallyGloballyUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SearchResultsViewModelTest {

    @Test
    fun givenBlankQuery_whenSearchQueryChanged_thenSearchIsNotTriggeredAndStateIsEmptyQuery() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.onSearchQueryChanged(" ")
        advanceUntilIdle()

        assertEquals(MessagesSearchState.EmptyQuery, viewModel.messagesSearchState.value)
        assertEquals(DiscussionsSearchState.EmptyQuery, viewModel.discussionsSearchState.value)
        coVerify(exactly = 0) { arrangement.searchMessagesSemanticallyGlobally(any(), any()) }
        coVerify(exactly = 0) { arrangement.identifyDiscussionTopicsFromSemanticSearch(any()) }
    }

    @Test
    fun givenSearchQuery_whenDebouncePasses_thenSemanticSearchIsCalledWithQuery() = runTest {
        val searchQuery = "invoice from alice"
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(emptyList()))
            .arrange()

        viewModel.onSearchQueryChanged(searchQuery)
        advanceDebounce()

        coVerify(exactly = 1) { arrangement.searchMessagesSemanticallyGlobally(searchQuery, any()) }
    }

    @Test
    fun givenSemanticSearchSuccess_whenMessagesAreReturned_thenMessagesAreMappedToUiState() = runTest {
        val message = TestMessage.TEXT_MESSAGE
        val uiMessage = mockMessageWithText
        val users = emptyList<User>()
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(message)))
            .withUsersForMessage(message, users)
            .withMappedMessage(users, message, uiMessage)
            .arrange()

        viewModel.onSearchQueryChanged("hello")
        advanceDebounce()

        val state = assertIs<MessagesSearchState.Success>(viewModel.messagesSearchState.value)
        assertEquals(listOf(uiMessage), state.messages)
        coVerify(exactly = 1) { arrangement.identifyDiscussionTopicsFromSemanticSearch(listOf(message)) }
    }

    @Test
    fun givenSemanticSearchSuccess_whenDiscussionTopicsAreReturned_thenDiscussionsStateIsSuccess() = runTest {
        val message = TestMessage.TEXT_MESSAGE
        val discussion = discussionSummary(topic = "Release planning")
        val users = emptyList<User>()
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(message)))
            .withDiscussionTopicsResult(listOf(message), listOf(discussion))
            .withUsersForMessage(message, users)
            .withMappedMessage(users, message, mockMessageWithText)
            .arrange()

        viewModel.onSearchQueryChanged("release")
        advanceDebounce()

        val state = assertIs<DiscussionsSearchState.Success>(viewModel.discussionsSearchState.value)
        assertEquals(listOf(discussion), state.discussions)
    }

    @Test
    fun givenTopicGenerationIsRunning_whenDiscussionMetadataIsReady_thenAllRowsAreShownWithoutTopics() = runTest {
        val message = TestMessage.TEXT_MESSAGE
        val pendingDiscussion = discussionSummary(topic = null)
        val completedDiscussion = pendingDiscussion.copy(topic = "Release planning")
        val users = emptyList<User>()
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(message)))
            .withIncrementalDiscussionTopicsResult(
                messages = listOf(message),
                initial = listOf(pendingDiscussion),
                completed = listOf(completedDiscussion)
            )
            .withUsersForMessage(message, users)
            .withMappedMessage(users, message, mockMessageWithText)
            .arrange()

        viewModel.onSearchQueryChanged("release")
        advanceTimeBy(DEFAULT_SEARCH_QUERY_DEBOUNCE)
        runCurrent()

        val pendingState = assertIs<DiscussionsSearchState.Success>(viewModel.discussionsSearchState.value)
        assertEquals(listOf(pendingDiscussion), pendingState.discussions)

        advanceUntilIdle()

        val completedState = assertIs<DiscussionsSearchState.Success>(viewModel.discussionsSearchState.value)
        assertEquals(listOf(completedDiscussion), completedState.discussions)
    }

    @Test
    fun givenSemanticSearchSuccess_whenNoDiscussionTopicsAreReturned_thenDiscussionsStateIsNoResults() = runTest {
        val message = TestMessage.TEXT_MESSAGE
        val users = emptyList<User>()
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(message)))
            .withDiscussionTopicsResult(listOf(message), emptyList())
            .withUsersForMessage(message, users)
            .withMappedMessage(users, message, mockMessageWithText)
            .arrange()

        viewModel.onSearchQueryChanged("release")
        advanceDebounce()

        assertEquals(DiscussionsSearchState.NoResults, viewModel.discussionsSearchState.value)
    }

    @Test
    fun givenSemanticSearchSuccessWithNoMessages_whenSearchCompletes_thenStateIsNoResults() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(SearchMessagesSemanticallyGloballyUseCase.Result.Success(emptyList()))
            .arrange()

        viewModel.onSearchQueryChanged("hello")
        advanceDebounce()

        assertEquals(MessagesSearchState.NoResults, viewModel.messagesSearchState.value)
        assertEquals(DiscussionsSearchState.NoResults, viewModel.discussionsSearchState.value)
        coVerify(exactly = 0) { arrangement.identifyDiscussionTopicsFromSemanticSearch(any()) }
    }

    @Test
    fun givenSemanticSearchFailure_whenSearchCompletes_thenStateIsFailure() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(
                SearchMessagesSemanticallyGloballyUseCase.Result.Failure(CoreFailure.Unknown(null))
            )
            .arrange()

        viewModel.onSearchQueryChanged("hello")
        advanceDebounce()

        assertEquals(MessagesSearchState.Failure, viewModel.messagesSearchState.value)
        assertEquals(DiscussionsSearchState.NoResults, viewModel.discussionsSearchState.value)
        coVerify(exactly = 0) { arrangement.identifyDiscussionTopicsFromSemanticSearch(any()) }
    }

    @Test
    fun givenRapidQueryChanges_whenDebouncePasses_thenOnlyLatestQueryIsSearched() = runTest {
        val firstQuery = "first"
        val secondQuery = "second"
        val firstMessage = TestMessage.TEXT_MESSAGE.copy(id = "first-message")
        val secondMessage = TestMessage.TEXT_MESSAGE.copy(id = "second-message")
        val users = emptyList<User>()
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(firstQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(firstMessage)))
            .withSemanticSearchResult(secondQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(secondMessage)))
            .withUsersForMessage(secondMessage, users)
            .withMappedMessage(users, secondMessage, mockMessageWithText)
            .arrange()

        viewModel.onSearchQueryChanged(firstQuery)
        advanceTimeBy(DEFAULT_SEARCH_QUERY_DEBOUNCE / 2)
        viewModel.onSearchQueryChanged(secondQuery)
        advanceDebounce()

        coVerify(exactly = 0) { arrangement.searchMessagesSemanticallyGlobally(firstQuery, any()) }
        coVerify(exactly = 1) { arrangement.searchMessagesSemanticallyGlobally(secondQuery, any()) }
        assertIs<MessagesSearchState.Success>(viewModel.messagesSearchState.value)
    }

    @Test
    fun givenPreviousSearchIsRunning_whenNewQueryStarts_thenLatestResultWins() = runTest {
        val firstQuery = "first"
        val secondQuery = "second"
        val firstMessage = TestMessage.TEXT_MESSAGE.copy(id = "first-message")
        val secondMessage = TestMessage.TEXT_MESSAGE.copy(id = "second-message")
        val users = emptyList<User>()
        val secondUiMessage = mockMessageWithText.copy(header = mockMessageWithText.header.copy(messageId = "second-message"))
        val (arrangement, viewModel) = Arrangement()
            .withDelayedSemanticSearchResult(firstQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(firstMessage)))
            .withSemanticSearchResult(secondQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(secondMessage)))
            .withUsersForMessage(secondMessage, users)
            .withMappedMessage(users, secondMessage, secondUiMessage)
            .arrange()

        viewModel.onSearchQueryChanged(firstQuery)
        advanceTimeBy(DEFAULT_SEARCH_QUERY_DEBOUNCE)
        runCurrent()
        viewModel.onSearchQueryChanged(secondQuery)
        advanceDebounce()

        val state = assertIs<MessagesSearchState.Success>(viewModel.messagesSearchState.value)
        assertEquals(listOf(secondUiMessage), state.messages)
    }

    @Test
    fun givenPreviousDiscussionTopicSearchIsRunning_whenNewQueryStarts_thenLatestDiscussionResultWins() = runTest {
        val firstQuery = "first"
        val secondQuery = "second"
        val firstMessage = TestMessage.TEXT_MESSAGE.copy(id = "first-message")
        val secondMessage = TestMessage.TEXT_MESSAGE.copy(id = "second-message")
        val users = emptyList<User>()
        val firstDiscussion = discussionSummary(topic = "First topic")
        val secondDiscussion = discussionSummary(topic = "Second topic")
        val secondUiMessage = mockMessageWithText.copy(header = mockMessageWithText.header.copy(messageId = "second-message"))
        val (arrangement, viewModel) = Arrangement()
            .withSemanticSearchResult(firstQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(firstMessage)))
            .withSemanticSearchResult(secondQuery, SearchMessagesSemanticallyGloballyUseCase.Result.Success(listOf(secondMessage)))
            .withDelayedDiscussionTopicsResult(listOf(firstMessage), listOf(firstDiscussion))
            .withDiscussionTopicsResult(listOf(secondMessage), listOf(secondDiscussion))
            .withUsersForMessage(firstMessage, users)
            .withUsersForMessage(secondMessage, users)
            .withMappedMessage(users, firstMessage, mockMessageWithText)
            .withMappedMessage(users, secondMessage, secondUiMessage)
            .arrange()

        viewModel.onSearchQueryChanged(firstQuery)
        advanceTimeBy(DEFAULT_SEARCH_QUERY_DEBOUNCE)
        runCurrent()
        viewModel.onSearchQueryChanged(secondQuery)
        advanceDebounce()

        val state = assertIs<DiscussionsSearchState.Success>(viewModel.discussionsSearchState.value)
        assertEquals(listOf(secondDiscussion), state.discussions)
    }

    private fun TestScope.advanceDebounce() {
        advanceTimeBy(DEFAULT_SEARCH_QUERY_DEBOUNCE)
        runCurrent()
        advanceUntilIdle()
    }

    private fun discussionSummary(topic: String?) = DiscussionClusterSummary(
        topic = topic,
        conversationId = ConversationId("conversation", "example.com"),
        firstMessageId = "first-message",
        conversationName = "Project Alpha",
        firstMessageDate = Instant.parse("2026-01-01T10:00:00Z"),
        lastMessageDate = Instant.parse("2026-01-01T11:00:00Z"),
        participants = listOf("Alice", "Bob")
    )

    private class Arrangement {
        @MockK
        lateinit var searchMessagesSemanticallyGlobally: SearchMessagesSemanticallyGloballyUseCase

        @MockK
        lateinit var identifyDiscussionTopicsFromSemanticSearch: IdentifyDiscussionTopicsFromSemanticSearchUseCase

        @MockK
        lateinit var getUsersForMessage: GetUsersForMessageUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { searchMessagesSemanticallyGlobally(any(), any()) } returns
                    SearchMessagesSemanticallyGloballyUseCase.Result.Success(emptyList())
            every { identifyDiscussionTopicsFromSemanticSearch(any()) } returns flowOf(emptyList())
            coEvery { getUsersForMessage(any()) } returns emptyList()
            every { messageMapper.toUIMessage(any(), any()) } returns null
        }

        fun withSemanticSearchResult(result: SearchMessagesSemanticallyGloballyUseCase.Result) = apply {
            coEvery { searchMessagesSemanticallyGlobally(any(), any()) } returns result
        }

        fun withSemanticSearchResult(
            searchQuery: String,
            result: SearchMessagesSemanticallyGloballyUseCase.Result
        ) = apply {
            coEvery { searchMessagesSemanticallyGlobally(searchQuery, any()) } returns result
        }

        fun withDelayedSemanticSearchResult(
            searchQuery: String,
            result: SearchMessagesSemanticallyGloballyUseCase.Result
        ) = apply {
            coEvery { searchMessagesSemanticallyGlobally(searchQuery, any()) } coAnswers {
                delay(DEFAULT_SEARCH_QUERY_DEBOUNCE * 2)
                result
            }
        }

        fun withDiscussionTopicsResult(
            messages: List<Message.Standalone>,
            result: List<DiscussionClusterSummary>
        ) = apply {
            every { identifyDiscussionTopicsFromSemanticSearch(messages) } returns flowOf(result)
        }

        fun withDelayedDiscussionTopicsResult(
            messages: List<Message.Standalone>,
            result: List<DiscussionClusterSummary>
        ) = apply {
            every { identifyDiscussionTopicsFromSemanticSearch(messages) } returns flow {
                emit(result.map { it.copy(topic = null) })
                delay(DEFAULT_SEARCH_QUERY_DEBOUNCE * 2)
                emit(result)
            }
        }

        fun withIncrementalDiscussionTopicsResult(
            messages: List<Message.Standalone>,
            initial: List<DiscussionClusterSummary>,
            completed: List<DiscussionClusterSummary>
        ) = apply {
            every { identifyDiscussionTopicsFromSemanticSearch(messages) } returns flow {
                emit(initial)
                delay(DEFAULT_SEARCH_QUERY_DEBOUNCE * 2)
                emit(completed)
            }
        }

        fun withUsersForMessage(message: Message, users: List<User>) = apply {
            coEvery { getUsersForMessage(message) } returns users
        }

        fun withMappedMessage(users: List<User>, message: Message.Standalone, uiMessage: UIMessage) = apply {
            every { messageMapper.toUIMessage(users, message) } returns uiMessage
        }

        fun arrange() = this to SearchResultsViewModel(
            searchMessagesSemanticallyGlobally = searchMessagesSemanticallyGlobally,
            identifyDiscussionTopicsFromSemanticSearch = identifyDiscussionTopicsFromSemanticSearch,
            getUsersForMessage = getUsersForMessage,
            messageMapper = messageMapper,
            dispatcher = TestDispatcherProvider(),
        )
    }
}
