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

import com.wire.android.navigation.BackStackMode
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SearchResultsScreenTest {

    @Test
    fun givenMessageSearchResult_whenNavigationCommandIsCreated_thenConversationIsOpenedAtSearchedMessage() {
        val messageId = "searched-message-id"
        val message = mockMessageWithText.copy(
            header = mockMessageWithText.header.copy(messageId = messageId)
        )

        val command = message.toConversationNavigationCommand()

        assertEquals(BackStackMode.UPDATE_EXISTED, command.backStackMode)
        assertTrue(command.destination.route.contains("searchedMessageId=$messageId"))
    }

    @Test
    fun givenDiscussionSearchResult_whenNavigationCommandIsCreated_thenConversationIsOpenedAtFirstClusterMessage() {
        val discussion = discussionSummary()

        val command = discussion.toConversationNavigationCommand()

        assertEquals(BackStackMode.UPDATE_EXISTED, command.backStackMode)
        assertTrue(command.destination.route.contains("searchedMessageId=${discussion.firstMessageId}"))
    }

    @Test
    fun givenParticipantFullNames_whenFormattingCard_thenOnlyFirstNamesAreReturned() {
        val discussion = discussionSummary().copy(
            participants = listOf("Paul Smith", "Vitor Oliveira", "Adrian Jones", "Jacob")
        )

        assertEquals(listOf("Paul", "Vitor", "Adrian", "Jacob"), discussion.participantFirstNames())
    }

    @Test
    fun givenDiscussionDates_whenFormattingCard_thenTodayYesterdayAndExactDateAreDistinguished() {
        val timeZone = TimeZone.UTC
        val currentDate = LocalDate(2026, 6, 18)

        assertEquals(
            DiscussionDateLabel.Today,
            Instant.parse("2026-06-18T10:00:00Z").toDiscussionDateLabel(currentDate, timeZone)
        )
        assertEquals(
            DiscussionDateLabel.Yesterday,
            Instant.parse("2026-06-17T10:00:00Z").toDiscussionDateLabel(currentDate, timeZone)
        )
        assertEquals(
            DiscussionDateLabel.Exact,
            Instant.parse("2026-06-16T10:00:00Z").toDiscussionDateLabel(currentDate, timeZone)
        )
    }

    private fun discussionSummary() = DiscussionClusterSummary(
        topic = "Release planning",
        conversationId = ConversationId("conversation", "example.com"),
        firstMessageId = "first-cluster-message",
        conversationName = "Project Alpha",
        firstMessageDate = Instant.parse("2026-06-18T10:00:00Z"),
        lastMessageDate = Instant.parse("2026-06-18T11:00:00Z"),
        participants = listOf("Alice", "Bob")
    )
}
