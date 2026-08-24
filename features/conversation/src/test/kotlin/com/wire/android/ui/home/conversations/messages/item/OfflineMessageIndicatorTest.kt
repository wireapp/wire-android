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

package com.wire.android.ui.home.conversations.messages.item

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OfflineMessageIndicatorTest {

    @Test
    fun `given offline state and no pending messages, when adding offline indicator, then indicator is the newest item`() = runTest {
        val message = regularMessage(id = "sent")

        val snapshot = flowOf(
            PagingData.from(listOf<UIMessage>(message)).withOfflineIndicator(TEST_CONVERSATION_ID, isOffline = true)
        ).asSnapshot()

        assertEquals(
            listOf("offline-message:$TEST_CONVERSATION_ID:before:sent", "sent"),
            snapshot.map { it.header.messageId }
        )
    }

    @Test
    fun `given offline state and pending messages, when adding offline indicator, then indicator is before pending block visually`() =
        runTest {
            val firstPending = regularMessage(id = "pending-1", flowStatus = MessageFlowStatus.Sending)
            val secondPending = regularMessage(id = "pending-2", flowStatus = MessageFlowStatus.Sending)
            val sent = regularMessage(id = "sent")

            val snapshot = flowOf(
                PagingData.from(listOf<UIMessage>(firstPending, secondPending, sent))
                    .withOfflineIndicator(TEST_CONVERSATION_ID, isOffline = true)
            ).asSnapshot()

            assertEquals(
                listOf("pending-1", "pending-2", "offline-message:$TEST_CONVERSATION_ID:after:pending-2", "sent"),
                snapshot.map { it.header.messageId }
            )
        }

    @Test
    fun `given offline state and multiple pending blocks, when adding offline indicator, then indicators are inserted with unique ids`() =
        runTest {
            val firstPending = regularMessage(id = "pending-1", flowStatus = MessageFlowStatus.Sending)
            val firstSent = regularMessage(id = "sent-1")
            val secondPending = regularMessage(id = "pending-2", flowStatus = MessageFlowStatus.Sending)
            val secondSent = regularMessage(id = "sent-2")

            val snapshot = flowOf(
                PagingData.from(listOf<UIMessage>(firstPending, firstSent, secondPending, secondSent))
                    .withOfflineIndicator(TEST_CONVERSATION_ID, isOffline = true)
            ).asSnapshot()

            assertEquals(
                listOf(
                    "pending-1",
                    "offline-message:$TEST_CONVERSATION_ID:after:pending-1",
                    "sent-1",
                    "pending-2",
                    "offline-message:$TEST_CONVERSATION_ID:after:pending-2",
                    "sent-2",
                ),
                snapshot.map { it.header.messageId },
            )
        }

    @Test
    fun `given online state, when adding offline indicator, then indicator is not inserted`() = runTest {
        val message = regularMessage(id = "sent")

        val snapshot = flowOf(
            PagingData.from(listOf<UIMessage>(message)).withOfflineIndicator(TEST_CONVERSATION_ID, isOffline = false)
        ).asSnapshot()

        assertEquals(listOf("sent"), snapshot.map { it.header.messageId })
    }

    @Test
    fun `given offline state and empty conversation, when adding offline indicator, then indicator is shown`() = runTest {
        val snapshot = flowOf(
            PagingData.from(emptyList<UIMessage>()).withOfflineIndicator(TEST_CONVERSATION_ID, isOffline = true)
        ).asSnapshot()

        assertEquals(listOf("offline-message:$TEST_CONVERSATION_ID:empty"), snapshot.map { it.header.messageId })
    }

    private fun regularMessage(
        id: String,
        flowStatus: MessageFlowStatus = MessageFlowStatus.Sent,
    ): UIMessage.Regular =
        UIMessage.Regular(
            conversationId = TEST_CONVERSATION_ID,
            header = MessageHeader(
                username = UIText.DynamicString(""),
                membership = Membership.None,
                showLegalHoldIndicator = false,
                messageTime = MessageTime(Clock.System.now()),
                messageStatus = MessageStatus(
                    flowStatus = flowStatus,
                    expirationStatus = ExpirationStatus.NotExpirable,
                ),
                messageId = id,
                userId = null,
                connectionState = null,
                isSenderDeleted = false,
                isSenderUnavailable = false,
            ),
            source = MessageSource.Self,
            userAvatarData = UserAvatarData(),
            messageContent = null,
            messageFooter = MessageFooter(id),
        )

    private companion object {
        val TEST_CONVERSATION_ID = ConversationId("conversation", "domain")
    }
}
