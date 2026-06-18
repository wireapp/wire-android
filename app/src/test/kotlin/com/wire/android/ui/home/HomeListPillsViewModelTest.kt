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
package com.wire.android.ui.home

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class HomeListPillsViewModelTest {

    @Test
    fun givenConversationsWithNewActivity_whenObserving_thenCountMatchesItemsWithNewActivity() = runTest {
        val (_, viewModel) = Arrangement()
            .withConversations(
                listOf(
                    detailsWithEvents(TestConversationDetails.GROUP, hasNewActivitiesToShow = true),
                    detailsWithEvents(TestConversationDetails.CONVERSATION_ONE_ONE, hasNewActivitiesToShow = true),
                    detailsWithEvents(TestConversationDetails.CONNECTION, hasNewActivitiesToShow = false),
                )
            )
            .arrange()

        viewModel.newActivityCount.test {
            assertEquals(2, awaitItem())
        }
    }

    @Test
    fun givenNoConversationsWithNewActivity_whenObserving_thenCountIsZero() = runTest {
        val (_, viewModel) = Arrangement()
            .withConversations(
                listOf(
                    detailsWithEvents(TestConversationDetails.GROUP, hasNewActivitiesToShow = false),
                    detailsWithEvents(TestConversationDetails.CONNECTION, hasNewActivitiesToShow = false),
                )
            )
            .arrange()

        viewModel.newActivityCount.test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun givenUseCaseFails_whenObserving_thenCountFallsBackToZero() = runTest {
        val (_, viewModel) = Arrangement()
            .withConversationsFlow(flow { throw IllegalStateException("boom") })
            .arrange()

        viewModel.newActivityCount.test {
            assertEquals(0, awaitItem())
        }
    }

    private fun detailsWithEvents(
        details: com.wire.kalium.logic.data.conversation.ConversationDetails,
        hasNewActivitiesToShow: Boolean,
    ) = ConversationDetailsWithEvents(
        conversationDetails = details,
        hasNewActivitiesToShow = hasNewActivitiesToShow,
    )

    private class Arrangement {

        @MockK
        lateinit var observeConversationListDetailsWithEvents: ObserveConversationListDetailsWithEventsUseCase

        private val conversationsFlow = MutableStateFlow<List<ConversationDetailsWithEvents>>(emptyList())

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeConversationListDetailsWithEvents(false, ConversationFilter.All) } returns conversationsFlow
        }

        fun withConversations(conversations: List<ConversationDetailsWithEvents>) = apply {
            conversationsFlow.value = conversations
        }

        fun withConversationsFlow(flow: kotlinx.coroutines.flow.Flow<List<ConversationDetailsWithEvents>>) = apply {
            coEvery { observeConversationListDetailsWithEvents(false, ConversationFilter.All) } returns flow
        }

        fun arrange() = this to HomeListPillsViewModel(
            observeConversationListDetailsWithEvents = observeConversationListDetailsWithEvents,
            dispatcher = TestDispatcherProvider(),
        )
    }
}
