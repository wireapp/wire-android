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
package com.wire.android.ui.home.conversations.messages.draft

import androidx.lifecycle.SavedStateHandle
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversation
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.usecase.ObserveQuoteMessageForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.ui.theme.Accent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.draft.ObserveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class, SnapshotExtension::class)
class MessageDraftViewModelTest {

    @Test
    fun `given message draft, when init, then state is properly updated`() = runTest {
        // given
        val messageDraft = MessageDraft(
            conversationId = TestConversation.ID,
            text = "hello",
            editMessageId = null,
            quotedMessageId = null,
            selectedMentionList = listOf()
        )
        val (arrangement, viewModel) = Arrangement()
            .withMessageDraft(messageDraft)
            .arrange()

        // when
        advanceUntilIdle()

        // then
        assertEquals(messageDraft.text, viewModel.state.value.draftText)
        coVerify(exactly = 1) {
            arrangement.observeMessageDraft(any())
        }
    }

    @Test
    fun `given null message draft, when init, then state is not updated`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withNoMessageDraft()
            .arrange()

        // when
        advanceUntilIdle()

        // then
        assertEquals(true, viewModel.state.value.draftText.isEmpty())
        coVerify(exactly = 1) {
            arrangement.observeMessageDraft(any())
        }
    }

    @Test
    fun `given message draft with quoted message, when init, then state is updated`() = runTest {
        // given
        val messageDraft = MessageDraft(
            conversationId = TestConversation.ID,
            text = "hello",
            editMessageId = null,
            quotedMessageId = "quoted_message_id",
            selectedMentionList = listOf()
        )
        val quotedData = UIQuotedMessage.UIQuotedData(
            messageId = "quoted_message_id",
            senderId = UserId("user_id", "domain"),
            senderName = UIText.DynamicString("John"),
            originalMessageDateDescription = UIText.StringResource(R.string.label_quote_original_message_date, "10:30"),
            editedTimeDescription = UIText.StringResource(R.string.label_message_status_edited_with_date, "10:32"),
            quotedContent = UIQuotedMessage.UIQuotedData.Text(UIText.DynamicString("Any ideas?")),
            senderAccent = Accent.Unknown
        )
        val (arrangement, viewModel) = Arrangement()
            .withMessageDraft(messageDraft)
            .withQuotedMessage(quotedData)
            .arrange()

        // when
        advanceUntilIdle()

        // then
        assertEquals(messageDraft.text, viewModel.state.value.draftText)
        assertEquals(messageDraft.quotedMessageId, viewModel.state.value.quotedMessageId)
        assertEquals(quotedData, viewModel.state.value.quotedMessage)

        coVerify(exactly = 1) {
            arrangement.observeMessageDraft(any())
        }
        coVerify(exactly = 1) {
            arrangement.observeQuoteMessageForConversation(any(), any())
        }
    }

    @Test
    fun `given message draft with unavailable quoted message, when init, then quoted data is not updated`() = runTest {
        // given
        val messageDraft = MessageDraft(
            conversationId = TestConversation.ID,
            text = "hello",
            editMessageId = null,
            quotedMessageId = "quoted_message_id",
            selectedMentionList = listOf()
        )
        val quotedData = UIQuotedMessage.UnavailableData

        val (arrangement, viewModel) = Arrangement()
            .withMessageDraft(messageDraft)
            .withQuotedMessage(quotedData)
            .arrange()

        // when
        advanceUntilIdle()

        // then
        assertEquals(messageDraft.text, viewModel.state.value.draftText)
        assertEquals(null, viewModel.state.value.quotedMessageId)

        coVerify(exactly = 1) {
            arrangement.observeMessageDraft(any())
        }
        coVerify(exactly = 1) {
            arrangement.observeQuoteMessageForConversation(any(), any())
        }
    }

    private class Arrangement {

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            every {
                savedStateHandle.navArgs<ConversationNavArgs>()
            } returns ConversationNavArgs(conversationId = TestConversation.ID)
        }

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var observeMessageDraft: ObserveMessageDraftUseCase

        @MockK
        lateinit var saveMessageDraft: SaveMessageDraftUseCase

        @MockK
        lateinit var observeQuoteMessageForConversation: ObserveQuoteMessageForConversationUseCase

        private val viewModel by lazy {
            MessageDraftViewModel(
                savedStateHandle,
                observeMessageDraft,
                observeQuoteMessageForConversation,
                saveMessageDraft,
            )
        }

        fun withNoMessageDraft() = apply {
            coEvery { observeMessageDraft(any()) } returns flowOf()
        }

        fun withMessageDraft(messageDraft: MessageDraft) = apply {
            coEvery { observeMessageDraft(any()) } returns flowOf(messageDraft)
        }

        fun withQuotedMessage(quotedMessage: UIQuotedMessage) = apply {
            coEvery { observeQuoteMessageForConversation(any(), any()) } returns flowOf(quotedMessage)
        }

        fun arrange() = this to viewModel
    }
}
