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
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.message.draft.MessageDraft
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@Suppress("LargeClass")
class MessageComposerViewModelTest {

    @Test
    fun `given that a user updates the self-deleting message timer, when invoked, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.HOURS)
            val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withPersistSelfDeletionStatus()
                .arrange()

            // When
            viewModel.updateSelfDeletingMessages(expectedTimer)

            // Then
            coVerify(exactly = 1) {
                arrangement.persistSelfDeletionStatus.invoke(
                    arrangement.conversationId,
                    expectedTimer
                )
            }
            assertInstanceOf(SelfDeletionTimer.Enabled::class.java, viewModel.messageComposerViewState.value.selfDeletionTimer)
            assertEquals(expectedDuration, viewModel.messageComposerViewState.value.selfDeletionTimer.duration)
        }

    @Test
    fun `given a valid observed enforced self-deleting message timer, when invoked, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.DAYS)
            val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withObserveSelfDeletingStatus(expectedTimer)
                .arrange()

            // When

            // Then
            coVerify(exactly = 1) {
                arrangement.observeConversationSelfDeletionStatus.invoke(
                    arrangement.conversationId,
                    true
                )
            }
            assertInstanceOf(SelfDeletionTimer.Enabled::class.java, viewModel.messageComposerViewState.value.selfDeletionTimer)
            assertEquals(expectedDuration, viewModel.messageComposerViewState.value.selfDeletionTimer.duration)
        }

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
    fun `given that user saves a draft message, then save draft use case is triggered`() =
        runTest {
            // given
            val messageDraft = MessageDraft(
                conversationId = ConversationId("value", "domain"),
                text = "hello",
                editMessageId = null,
                quotedMessageId = null,
                selectedMentionList = listOf()
            )
            val (arrangement, viewModel) = MessageComposerViewModelArrangement()
                .withSuccessfulViewModelInit()
                .withSaveDraftMessage()
                .arrange()

            // when
            viewModel.saveDraft(messageDraft)
            advanceUntilIdle()

            // then
            coVerify(exactly = 1) { arrangement.saveMessageDraftUseCase.invoke(eq(messageDraft)) }
        }
}
