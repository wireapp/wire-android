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

package com.wire.android.ui.home.conversations.edit

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageOptionsModalSheetLayoutTest {

    @Test
    fun givenPendingMessageAndPendingMessagesDisabled_whenCheckingEditOption_thenItIsNotAvailable() {
        val result = isMessageEditOptionAvailable(
            isDeleted = false,
            isContentEditable = true,
            isMyMessage = true,
            isPending = true,
            isNetworkAvailable = false,
            pendingMessagesEnabled = false,
        )

        assertFalse(result)
    }

    @Test
    fun givenPendingMessageAndPendingMessagesEnabledAndOffline_whenCheckingEditOption_thenItIsAvailable() {
        val result = isMessageEditOptionAvailable(
            isDeleted = false,
            isContentEditable = true,
            isMyMessage = true,
            isPending = true,
            isNetworkAvailable = false,
            pendingMessagesEnabled = true,
        )

        assertTrue(result)
    }

    @Test
    fun givenNonPendingEditableSelfMessage_whenCheckingEditOption_thenItIsAvailable() {
        val result = isMessageEditOptionAvailable(
            isDeleted = false,
            isContentEditable = true,
            isMyMessage = true,
            isPending = false,
            isNetworkAvailable = true,
            pendingMessagesEnabled = false,
        )

        assertTrue(result)
    }
}
