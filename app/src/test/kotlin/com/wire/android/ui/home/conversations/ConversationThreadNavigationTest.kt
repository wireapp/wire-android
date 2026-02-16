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

package com.wire.android.ui.home.conversations

import com.ramcosta.composedestinations.generated.app.destinations.ThreadConversationScreenDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.kalium.logic.data.id.ConversationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ConversationThreadNavigationTest {

    @Test
    fun `given thread params, when creating thread navigation command, then destination is thread conversation destination`() {
        val conversationId = ConversationId("conversation-id", "wire.com")
        val threadId = "thread-id"
        val rootMessageId = "root-message-id"
        val rootSelfDeletionDurationMillis = 5_000L

        val command = threadNavigationCommand(
            conversationId = conversationId,
            threadId = threadId,
            rootMessageId = rootMessageId,
            rootMessageSelfDeletionDurationMillis = rootSelfDeletionDurationMillis
        )

        val expected = NavigationCommand(
            ThreadConversationScreenDestination(
                ThreadConversationNavArgs(
                    conversationId = conversationId,
                    threadId = threadId,
                    threadRootMessageId = rootMessageId,
                    threadRootSelfDeletionDurationMillis = rootSelfDeletionDurationMillis
                )
            ),
            launchSingleTop = false
        )

        assertEquals(expected, command)
    }

    @Test
    fun `conversation nav args does not contain legacy thread fields`() {
        val fieldNames = ConversationNavArgs::class.java.declaredFields.map { it.name }

        assertFalse(fieldNames.contains("threadId"))
        assertFalse(fieldNames.contains("threadRootMessageId"))
        assertFalse(fieldNames.contains("threadRootSelfDeletionDurationMillis"))
    }
}
