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
}
