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

import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageSwipeActionTest {

    @Test
    fun `given stored value is null, when parsing swipe action, then should return default value`() {
        val result = MessageSwipeAction.fromStoredValue(null, MessageSwipeAction.DEFAULT_RIGHT)

        assertEquals(MessageSwipeAction.DEFAULT_RIGHT, result)
    }

    @Test
    fun `given stored value is unknown, when parsing swipe action, then should return default value`() {
        val result = MessageSwipeAction.fromStoredValue("STAR", MessageSwipeAction.DEFAULT_LEFT)

        assertEquals(MessageSwipeAction.DEFAULT_LEFT, result)
    }

    @Test
    fun `given stored value is known, when parsing swipe action, then should return matching value`() {
        val result = MessageSwipeAction.fromStoredValue(MessageSwipeAction.DETAILS.name, MessageSwipeAction.DEFAULT_LEFT)

        assertEquals(MessageSwipeAction.DETAILS, result)
    }

    @Test
    fun `given message is replyable, when mapping reply swipe action, then should return action`() {
        var wasReplyClicked = false

        val swipeAction = MessageSwipeAction.REPLY.toSwipeAction(
            message = mockMessageWithText,
            onSwipedToReply = { wasReplyClicked = true },
            onSwipedToReact = {},
            onSwipedToDetails = {},
        )

        assertNotNull(swipeAction)
        swipeAction?.action?.invoke()
        assertTrue(wasReplyClicked)
    }

    @Test
    fun `given message is not replyable, when mapping reply swipe action, then should return null`() {
        val swipeAction = MessageSwipeAction.REPLY.toSwipeAction(
            message = pendingMessage,
            onSwipedToReply = {},
            onSwipedToReact = {},
            onSwipedToDetails = {},
        )

        assertNull(swipeAction)
    }

    @Test
    fun `given reaction is allowed, when mapping react swipe action, then should return action`() {
        var wasReactClicked = false

        val swipeAction = MessageSwipeAction.REACT.toSwipeAction(
            message = mockMessageWithText,
            onSwipedToReply = {},
            onSwipedToReact = { wasReactClicked = true },
            onSwipedToDetails = {},
        )

        assertNotNull(swipeAction)
        swipeAction?.action?.invoke()
        assertTrue(wasReactClicked)
    }

    @Test
    fun `given reaction is not allowed, when mapping react swipe action, then should return null`() {
        val swipeAction = MessageSwipeAction.REACT.toSwipeAction(
            message = pendingMessage,
            onSwipedToReply = {},
            onSwipedToReact = {},
            onSwipedToDetails = {},
        )

        assertNull(swipeAction)
    }

    @Test
    fun `given message is not replyable or reactable, when mapping details swipe action, then should return action`() {
        var wasDetailsClicked = false

        val swipeAction = MessageSwipeAction.DETAILS.toSwipeAction(
            message = pendingMessage,
            onSwipedToReply = {},
            onSwipedToReact = {},
            onSwipedToDetails = { wasDetailsClicked = true },
        )

        assertFalse(pendingMessage.isReplyable)
        assertFalse(pendingMessage.isReactionAllowed)
        assertNotNull(swipeAction)
        swipeAction?.action?.invoke()
        assertTrue(wasDetailsClicked)
    }

    private val pendingMessage = mockMessageWithText.copy(
        header = mockMessageWithText.header.copy(
            messageStatus = mockMessageWithText.header.messageStatus.copy(
                flowStatus = MessageFlowStatus.Sending
            )
        )
    )
}
