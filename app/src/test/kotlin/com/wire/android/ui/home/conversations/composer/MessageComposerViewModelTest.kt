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

package com.wire.android.ui.home.conversations.composer

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@Suppress("LargeClass")
class MessageComposerViewModelTest {

    @Test
    fun `given that user types a text message, when invoked typing invoked, then send typing event is called`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()

        // when
        viewModel.sendTypingEvent(Conversation.TypingIndicatorMode.STARTED)

        // then
        coVerify(exactly = 1) {
            arrangement.sendTypingEvent.invoke(
                any(),
                eq(Conversation.TypingIndicatorMode.STARTED)
            )
        }
    }

    @Test
    fun `given no current session, then disable interaction`() = runTest {
        // given
        val (_, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withCurrentSessionFlowResult(flowOf(CurrentSessionResult.Failure.SessionNotFound))
            .arrange()
        advanceUntilIdle()
        // then
        assertEquals(InteractionAvailability.DISABLED, viewModel.messageComposerViewState.value.interactionAvailability)
    }

    @Test
    fun `given enter to send is enabled, when init, then update state`() = runTest {
        // given
        val (_, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit(enterToSend = true)
            .arrange()
        // when

        // then
        assertTrue(viewModel.messageComposerViewState.value.enterToSend)
    }

    @Test
    fun `given enter to send is disabled, when init, then update state`() = runTest {
        // given
        val (_, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit(enterToSend = false)
            .arrange()
        // when

        // then
        assertTrue(!viewModel.messageComposerViewState.value.enterToSend)
    }

    @Test
    fun `given messages were viewed, when conversation is closed, then mark as read with last viewed timestamp`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()
        val timestamp = "2024-01-15T10:30:00Z"
        val expectedInstant = Instant.parse(timestamp)

        // when
        viewModel.updateConversationReadDate(timestamp)
        advanceUntilIdle()
        viewModel.onConversationClosed()
        advanceUntilIdle()

        // then
        coVerify(exactly = 1) {
            arrangement.markConversationAsReadLocallyUseCase(
                arrangement.conversationId,
                expectedInstant
            )
        }
    }

    @Test
    fun `given no messages were viewed, when conversation is closed, then mark as read is not called`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()
        advanceUntilIdle()

        // when - close without calling updateConversationReadDate
        viewModel.onConversationClosed()
        advanceUntilIdle()

        // then
        coVerify(exactly = 0) {
            arrangement.markConversationAsReadLocallyUseCase(any(), any())
        }
    }

    @Test
    fun `given multiple read date updates, when conversation is closed, then use the most recent timestamp`() = runTest {
        // given
        val (arrangement, viewModel) = MessageComposerViewModelArrangement()
            .withSuccessfulViewModelInit()
            .arrange()
        val olderTimestamp = "2024-01-15T10:00:00Z"
        val newerTimestamp = "2024-01-15T10:30:00Z"
        val expectedInstant = Instant.parse(newerTimestamp)

        // when
        viewModel.updateConversationReadDate(olderTimestamp)
        viewModel.updateConversationReadDate(newerTimestamp)
        advanceUntilIdle()
        viewModel.onConversationClosed()
        advanceUntilIdle()

        // then
        coVerify(exactly = 1) {
            arrangement.markConversationAsReadLocallyUseCase(
                arrangement.conversationId,
                expectedInstant
            )
        }
    }
}
