/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations.messages.item

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageStyleTest {
    @Test
    fun givenMessageStyle_whenCheckingBubbleMembership_thenOnlyBubbleStylesAreBubbles() {
        assertTrue(MessageStyle.BUBBLE_SELF.isBubble())
        assertTrue(MessageStyle.BUBBLE_OTHER.isBubble())
        assertFalse(MessageStyle.NORMAL.isBubble())
    }

    @Test
    fun givenMessageStyle_whenResolvingAlpha_thenBubbleStylesUseTheSharedOpacity() {
        assertEquals(0.7F, MessageStyle.BUBBLE_SELF.alpha())
        assertEquals(0.7F, MessageStyle.BUBBLE_OTHER.alpha())
        assertEquals(1F, MessageStyle.NORMAL.alpha())
    }
}
