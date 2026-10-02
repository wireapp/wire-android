/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageComposerKeyboardActionsTest {

    @Test
    fun `given enter to send is off then keyboard uses default action handling`() {
        assertNull(
            messageComposerKeyboardActionHandler(enterToSend = false, canSendMessage = true, onSend = {
                error("Message must not be sent")
            })
        )
    }

    @Test
    fun `given enter to send is on when keyboard action fires then sends exactly once`() {
        var sends = 0
        val handler = messageComposerKeyboardActionHandler(enterToSend = true, canSendMessage = true, onSend = { sends++ })

        requireNotNull(handler).onKeyboardAction { error("Must not invoke default action") }

        assertEquals(1, sends)
    }

    @Test
    fun `given message cannot be sent when keyboard action fires then does not send`() {
        val handler = messageComposerKeyboardActionHandler(enterToSend = true, canSendMessage = false, onSend = {
            error("Message must not be sent")
        })

        requireNotNull(handler).onKeyboardAction { error("Must not invoke default action") }
    }

    @Test
    fun `given enter to send is off when Enter is pressed then leaves newline to text field`() {
        assertFalse(
            handleMessageComposerEnter(
                enterToSend = false,
                isShiftPressed = false,
                canSendMessage = true,
                onNewLine = { error("Text field should insert newline") },
                onSend = { error("Message must not be sent") },
            )
        )
    }

    @Test
    fun `given enter to send is on when Enter is pressed then sends exactly once and consumes key`() {
        var sends = 0

        assertTrue(
            handleMessageComposerEnter(
                enterToSend = true,
                isShiftPressed = false,
                canSendMessage = true,
                onNewLine = { error("Must not insert newline") },
                onSend = { sends++ },
            )
        )
        assertEquals(1, sends)
    }

    @Test
    fun `given message cannot be sent when Enter is pressed then does not send`() {
        assertFalse(
            handleMessageComposerEnter(
                enterToSend = true,
                isShiftPressed = false,
                canSendMessage = false,
                onNewLine = { error("Must not insert newline") },
                onSend = { error("Message must not be sent") },
            )
        )
    }

    @Test
    fun `when Shift Enter is pressed then preserves explicit newline handling for either setting`() {
        listOf(false, true).forEach { enterToSend ->
            var newlines = 0
            assertTrue(
                handleMessageComposerEnter(
                    enterToSend = enterToSend,
                    isShiftPressed = true,
                    canSendMessage = true,
                    onNewLine = { newlines++ },
                    onSend = { error("Message must not be sent") },
                )
            )
            assertEquals(1, newlines)
        }
    }
}
