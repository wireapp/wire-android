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

package com.wire.android.ui.home.conversations.model

import com.wire.android.util.ui.UIText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UIMessageTest {

    @Test
    fun `last message preview defaults preserve the HTML non-breaking-space separator`() {
        val text = UIText.DynamicString("message")

        assertEquals(
            "&nbsp;",
            UILastMessageContent.SenderWithMessage(sender = text, message = text).separator,
        )
        assertEquals(
            "&nbsp;",
            UILastMessageContent.MultipleMessage(messages = listOf(text)).separator,
        )
        assertEquals(
            ": ",
            UILastMessageContent.SenderWithMessage(sender = text, message = text, separator = ": ").separator,
        )
    }

    @Test
    fun `last message preview separator survives serialization`() {
        val text = UIText.DynamicString("message")
        val previews = listOf<UILastMessageContent>(
            UILastMessageContent.SenderWithMessage(sender = text, message = text),
            UILastMessageContent.MultipleMessage(messages = listOf(text)),
        )

        previews.forEach { preview ->
            val restored = Json.decodeFromString<UILastMessageContent>(Json.encodeToString(preview))

            assertEquals(preview, restored)
        }
    }

    @Test
    fun `quoted message body survives serialization`() {
        val body = MessageBody(
            message = UIText.DynamicString("message"),
            quotedMessage = UIQuotedMessage.UnavailableData,
        )

        val restored = Json.decodeFromString<MessageBody>(Json.encodeToString(body))

        assertEquals(body, restored)
    }
}
