/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.messages

import android.os.Bundle
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesNavArgs
import com.wire.android.ui.home.conversations.search.messages.SearchConversationMessagesViewModel
import com.wire.android.ui.home.conversations.usecase.GetConversationMessagesFromSearchUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class, SnapshotExtension::class)
class SearchConversationMessagesViewModelTest {

    @Test
    fun `given search term, when searching for messages, then specific messages are returned`() = runTest {
        // given
        val message1 = mockMessageWithText.copy(
            messageContent = UIMessageContent.TextMessage(
                messageBody = MessageBody(
                    UIText.DynamicString("message1")
                )
            )
        )

        val (arrangement, viewModel) = SearchConversationMessagesViewModelArrangement()
            .arrange()

        // when
        arrangement.withSuccessSearch(pagingDataFlow = PagingData.from(listOf(message1)))

        // then
        viewModel.searchConversationMessagesState.searchResult.test {
            awaitItem().map {
                it shouldBeEqualTo message1
            }
        }
    }

    @Test
    fun `given blank search term, when searching for messages, then search is not triggered`() = runTest {
        // given
        val searchTerm = " "
        val messages = listOf(
            mockMessageWithText.copy(
                messageContent = UIMessageContent.TextMessage(
                    messageBody = MessageBody(
                        UIText.DynamicString("  message1")
                    )
                )
            ),
            mockMessageWithText.copy(
                messageContent = UIMessageContent.TextMessage(
                    messageBody = MessageBody(
                        UIText.DynamicString("  message2")
                    )
                )
            )
        )

        val (arrangement, viewModel) = SearchConversationMessagesViewModelArrangement()
            .withSuccessSearch(PagingData.from(messages))
            .arrange()

        // when
        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(searchTerm)
        advanceUntilIdle()

        // then
        assertEquals(searchTerm, viewModel.searchQueryTextState.text.toString())
        coVerify(exactly = 0) {
            arrangement.getSearchMessagesForConversation(
                searchTerm,
                arrangement.conversationId,
                any()
            )
        }
    }

    @Test
    fun `given search term with space, when searching for messages, then search results are as expected`() = runTest {
        // given
        val searchTerm = "no "
        val message1 = mockMessageWithText.copy(
            messageContent = UIMessageContent.TextMessage(
                messageBody = MessageBody(
                    UIText.DynamicString("not a normal text")
                )
            )
        )
        val message2 = mockMessageWithText.copy(
            messageContent = UIMessageContent.TextMessage(
                messageBody = MessageBody(
                    UIText.DynamicString("this message contains a no message")
                )
            )
        )

        val messages = listOf(
            message1,
            message2
        )

        val (arrangement, viewModel) = SearchConversationMessagesViewModelArrangement()
            .withSuccessSearch(PagingData.from(messages))
            .arrange()

        // when
        viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(searchTerm)
        advanceUntilIdle()

        // then
        Assertions.assertEquals(searchTerm, viewModel.searchQueryTextState.text.toString())
        coVerify(exactly = 0) {
            arrangement.getSearchMessagesForConversation(
                searchTerm,
                arrangement.conversationId,
                any()
            )
        }
        viewModel.searchConversationMessagesState.searchResult.test {
            awaitItem().map {
                it shouldBeEqualTo message2
            }
        }
    }

    @Test
    fun `given search term with empty space at start and at end, when searching for messages, then specific messages are returned`() =
        runTest {
            // given
            val searchTerm = "message"
            val message1 = mockMessageWithText.copy(
                messageContent = UIMessageContent.TextMessage(
                    messageBody = MessageBody(
                        UIText.DynamicString(" message1 ")
                    )
                )
            )

            val (arrangement, viewModel) = SearchConversationMessagesViewModelArrangement()
                .arrange()

            // when
            viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(searchTerm)

            // then
            viewModel.searchConversationMessagesState.searchResult.test {
                arrangement.withSuccessSearch(pagingDataFlow = PagingData.from(listOf(message1)))
                awaitItem().map {
                    it shouldBeEqualTo message1
                }
            }
        }

    class SearchConversationMessagesViewModelArrangement {
        val conversationId: ConversationId = ConversationId(
            value = "some-dummy-value",
            domain = "some-dummy-domain"
        )

        private val messagesChannel = Channel<PagingData<UIMessage>>(capacity = Channel.UNLIMITED)

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getSearchMessagesForConversation: GetConversationMessagesFromSearchUseCase

        private val viewModel: SearchConversationMessagesViewModel by lazy {
            SearchConversationMessagesViewModel(
                getSearchMessagesForConversation = getSearchMessagesForConversation,
                dispatchers = TestDispatcherProvider(),
                savedStateHandle = savedStateHandle
            )
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            every { savedStateHandle.navArgs<SearchConversationMessagesNavArgs>() } returns SearchConversationMessagesNavArgs(
                conversationId = conversationId
            )
            every { savedStateHandle.get<Bundle?>("searchConversationMessagesState") } returns bundleOf(
                "value" to ""
            )
            coEvery {
                getSearchMessagesForConversation(
                    any(),
                    any(),
                    any()
                )
            } returns messagesChannel.consumeAsFlow()
        }

        suspend fun withSuccessSearch(
            pagingDataFlow: PagingData<UIMessage>
        ) = apply {
            messagesChannel.send(pagingDataFlow)
        }

        fun arrange() = this to viewModel
    }
}
