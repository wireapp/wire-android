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
package com.wire.android.ui.home.conversations.messages

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.mock.mockMessageWithTextContent
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.theme.Accent
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationMessagesViewModelReplyHighlightTest {

    @Test
    fun givenOriginalMessageFound_whenNavigatingToReply_thenReplyHighlightIsRequestedAndSearchHighlightIsUntouched() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 42))
            .arrange()
        advanceUntilIdle()
        val messagesBefore = viewModel.conversationViewState.messages

        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()

        val state = viewModel.conversationViewState
        assertEquals(ORIGINAL_ID, state.replyHighlightRequest?.messageId)
        assertEquals(41, state.firstUnreadEventIndex)
        assertNotSame(messagesBefore, state.messages)
        assertNull(state.searchedMessageId)
    }

    @Test
    fun givenRequestIssued_whenHighlightFinishes_thenRequestIsCleared() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 3))
            .arrange()
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        val request = viewModel.conversationViewState.replyHighlightRequest!!

        viewModel.onReplyHighlightFinished(request.token)

        assertNull(viewModel.conversationViewState.replyHighlightRequest)
    }

    @Test
    fun givenSameReplyTappedTwice_whenNavigating_thenEachTapIssuesANewToken() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 3))
            .arrange()

        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        val first = viewModel.conversationViewState.replyHighlightRequest!!
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        val second = viewModel.conversationViewState.replyHighlightRequest!!

        assertEquals(first.messageId, second.messageId)
        assertNotEquals(first.token, second.token)
    }

    @Test
    fun givenNewerRequest_whenStaleHighlightFinishes_thenNewerRequestIsKept() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 3))
            .arrange()
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        val stale = viewModel.conversationViewState.replyHighlightRequest!!
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        val current = viewModel.conversationViewState.replyHighlightRequest!!

        viewModel.onReplyHighlightFinished(stale.token)

        assertEquals(current, viewModel.conversationViewState.replyHighlightRequest)
    }

    @Test
    fun givenSlowFirstLookup_whenTappingAnotherReply_thenOnlyTheLatestRequestLands() = runTest {
        val slowLookup = CompletableDeferred<GetSearchedConversationMessagePositionUseCase.Result>()
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, slowLookup)
            .withSearchedMessagePosition(OTHER_ORIGINAL_ID, success(position = 7))
            .arrange()
        advanceUntilIdle()

        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()
        viewModel.navigateToReplyOriginalMessage(replyTo(OTHER_ORIGINAL_ID))
        advanceUntilIdle()
        slowLookup.complete(success(position = 99))
        advanceUntilIdle()

        val state = viewModel.conversationViewState
        assertEquals(OTHER_ORIGINAL_ID, state.replyHighlightRequest?.messageId)
        assertEquals(6, state.firstUnreadEventIndex)
    }

    @Test
    fun givenOriginalMessageNotFound_whenNavigatingToReply_thenNothingIsHighlightedAndListStays() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(
                messageId = ORIGINAL_ID,
                result = GetSearchedConversationMessagePositionUseCase.Result.Failure(StorageFailure.DataNotFound)
            )
            .arrange()
        advanceUntilIdle()
        val stateBefore = viewModel.conversationViewState

        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()

        assertNull(viewModel.conversationViewState.replyHighlightRequest)
        assertEquals(stateBefore.messages, viewModel.conversationViewState.messages)
        assertEquals(stateBefore.firstUnreadEventIndex, viewModel.conversationViewState.firstUnreadEventIndex)
    }

    @Test
    fun givenLookupInFlight_whenLeavingConversation_thenNoRequestLandsAfterwards() = runTest {
        val slowLookup = CompletableDeferred<GetSearchedConversationMessagePositionUseCase.Result>()
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, slowLookup)
            .arrange()
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()

        viewModel.clearReplyHighlight()
        slowLookup.complete(success(position = 3))
        advanceUntilIdle()

        assertNull(viewModel.conversationViewState.replyHighlightRequest)
    }

    @Test
    fun givenRequestIssued_whenLeavingConversation_thenRequestIsCleared() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 3))
            .arrange()
        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()

        viewModel.clearReplyHighlight()

        assertNull(viewModel.conversationViewState.replyHighlightRequest)
    }

    @Test
    fun givenSearchedMessageNavArg_whenTimeoutPasses_thenSearchHighlightIsClearedAndNoReplyRequestIsIssued() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessageIdNavArg(ORIGINAL_ID)
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 3))
            .arrange()

        advanceTimeBy(1_000)
        assertEquals(ORIGINAL_ID, viewModel.conversationViewState.searchedMessageId)
        assertNull(viewModel.conversationViewState.replyHighlightRequest)

        advanceUntilIdle()
        assertNull(viewModel.conversationViewState.searchedMessageId)
    }

    @Test
    fun givenSearchHighlightPending_whenNavigatingToReply_thenSearchHighlightIsReplacedByReplyHighlight() = runTest {
        val (_, viewModel) = ConversationMessagesViewModelArrangement()
            .withSuccessfulViewModelInit()
            .withSearchedMessageIdNavArg(OTHER_ORIGINAL_ID)
            .withSearchedMessagePosition(OTHER_ORIGINAL_ID, success(position = 3))
            .withSearchedMessagePosition(ORIGINAL_ID, success(position = 5))
            .arrange()
        advanceTimeBy(1_000)

        viewModel.navigateToReplyOriginalMessage(replyTo(ORIGINAL_ID))
        advanceUntilIdle()

        assertNull(viewModel.conversationViewState.searchedMessageId)
        assertEquals(ORIGINAL_ID, viewModel.conversationViewState.replyHighlightRequest?.messageId)
    }

    private fun success(position: Int) = GetSearchedConversationMessagePositionUseCase.Result.Success(position)

    private fun replyTo(originalMessageId: String): UIMessage = mockMessageWithTextContent("reply").copy(
        messageContent = UIMessageContent.TextMessage(
            messageBody = MessageBody(
                message = UIText.DynamicString("reply"),
                quotedMessage = UIQuotedMessage.UIQuotedData(
                    messageId = originalMessageId,
                    senderId = UserId("user", "domain"),
                    senderName = UIText.DynamicString("Sender Name"),
                    senderAccent = Accent.Unknown,
                    originalMessageDateDescription = UIText.DynamicString("Date"),
                    editedTimeDescription = null,
                    quotedContent = UIQuotedMessage.UIQuotedData.Text(UIText.DynamicString("original")),
                )
            )
        )
    )

    private companion object {
        const val ORIGINAL_ID = "original-message-id"
        const val OTHER_ORIGINAL_ID = "other-original-message-id"
    }
}
