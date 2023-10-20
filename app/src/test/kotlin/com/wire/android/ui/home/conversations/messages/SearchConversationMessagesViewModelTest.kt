/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
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
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SearchConversationMessagesViewModelTest {

    @Test
    fun `given search term, when searching for messages, then specific messages are returned`() =
        runTest() {
            // given
            val searchTerm = "message"
            val messages = listOf(
                mockMessageWithText.copy(
                    messageContent = UIMessageContent.TextMessage(
                        messageBody = MessageBody(
                            UIText.DynamicString("message1")
                        )
                    )
                ),
                mockMessageWithText.copy(
                    messageContent = UIMessageContent.TextMessage(
                        messageBody = MessageBody(
                            UIText.DynamicString("message2")
                        )
                    )
                )
            )

            val (arrangement, viewModel) = SearchConversationMessagesViewModelArrangement()
                .withSuccessSearch(searchTerm, messages)
                .arrange()

            // when
            viewModel.searchQueryChanged(TextFieldValue(searchTerm))
            advanceUntilIdle()

            // then
            assertEquals(
                TextFieldValue(searchTerm),
                viewModel.searchConversationMessagesState.searchQuery
            )
            coVerify(exactly = 1) {
                arrangement.getSearchMessagesForConversation(
                    searchTerm,
                    arrangement.conversationId
                )
            }
        }

    class SearchConversationMessagesViewModelArrangement {
        val conversationId: ConversationId = ConversationId(
            value = "some-dummy-value",
            domain = "some-dummy-domain"
        )

        private val messagesChannel =
            Channel<Either<CoreFailure, List<UIMessage>>>(capacity = Channel.UNLIMITED)

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var getSearchMessagesForConversation: GetConversationMessagesFromSearchUseCase

        private val viewModel: SearchConversationMessagesViewModel by lazy {
            SearchConversationMessagesViewModel(
                getSearchMessagesForConversation = getSearchMessagesForConversation,
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
//            coEvery {
//                getSearchMessagesForConversation(any(), any())
//            } returns messagesChannel.consume {
//                this.receive().right(listOf())
//            }
        }

        suspend fun withSuccessSearch(
            searchTerm: String,
            messages: List<UIMessage>
        ) = apply {
            coEvery {
                getSearchMessagesForConversation(eq(searchTerm), eq(conversationId))
            } returns Either.Right(messages)
        }

        suspend fun withErrorSearch(
            searchTerm: String,
            error: CoreFailure // StorageFailure.DataNotFound
        ) = apply {
            coEvery {
                getSearchMessagesForConversation(eq(searchTerm), eq(conversationId))
            } returns Either.Left(error)
        }

        fun arrange() = this to viewModel
    }
}
