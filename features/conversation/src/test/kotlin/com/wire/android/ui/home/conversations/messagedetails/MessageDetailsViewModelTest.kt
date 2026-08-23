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

package com.wire.android.ui.home.conversations.messagedetails

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.mapper.testUIParticipant
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCase
import com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCase
import com.wire.kalium.logic.data.message.receipt.ReceiptType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageDetailsViewModelTest {

    @Test
    fun givenNavigationArgsAndDetailFlows_whenCreated_thenReflectsArgsAndBothProjections() = runTest {
        val reactions = MessageDetailsReactionsData(
            reactions = mapOf("😀" to listOf(testUIParticipant(1))),
        )
        val readReceipts = MessageDetailsReadReceiptsData(
            readReceipts = listOf(testUIParticipant(2)),
        )
        val (arrangement, viewModel) = Arrangement()
            .withReactions(reactions)
            .withReadReceipts(readReceipts)
            .arrange()

        assertEquals(true, viewModel.messageDetailsState.isSelfMessage)
        assertEquals(reactions, viewModel.messageDetailsState.reactionsData)
        assertEquals(readReceipts, viewModel.messageDetailsState.readReceiptsData)
        coVerify(exactly = 1) {
            arrangement.observeReactionsForMessage(TestConversation.ID, MESSAGE_ID)
        }
        coVerify(exactly = 1) {
            arrangement.observeReceiptsForMessage(TestConversation.ID, MESSAGE_ID, ReceiptType.READ)
        }
    }

    private class Arrangement {

        @MockK
        lateinit var observeReactionsForMessage: ObserveReactionsForMessageUseCase

        @MockK
        lateinit var observeReceiptsForMessage: ObserveReceiptsForMessageUseCase

        init {
            MockKAnnotations.init(this)
            withReactions(MessageDetailsReactionsData())
            withReadReceipts(MessageDetailsReadReceiptsData())
        }

        fun withReactions(data: MessageDetailsReactionsData) = apply {
            coEvery { observeReactionsForMessage(TestConversation.ID, MESSAGE_ID) } returns flowOf(data)
        }

        fun withReadReceipts(data: MessageDetailsReadReceiptsData) = apply {
            coEvery {
                observeReceiptsForMessage(TestConversation.ID, MESSAGE_ID, ReceiptType.READ)
            } returns flowOf(data)
        }

        fun arrange() = this to MessageDetailsViewModel(
            navigationArgs = MessageDetailsNavArgs(
                conversationId = TestConversation.ID,
                messageId = MESSAGE_ID,
                isSelfMessage = true,
            ),
            observeReactionsForMessage = observeReactionsForMessage,
            observeReceiptsForMessage = observeReceiptsForMessage,
        )
    }

    private companion object {
        const val MESSAGE_ID = "message-id"
    }
}
