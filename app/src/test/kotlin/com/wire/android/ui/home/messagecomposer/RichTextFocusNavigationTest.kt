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
import org.junit.jupiter.api.Test

class RichTextFocusNavigationTest {

    @Test
    fun givenLastOptionFocused_whenMovingNext_thenFocusWrapsToFirstOption() {
        assertEquals(0, richTextFocusTargetIndex(3, 4, RichTextFocusMove.Next))
    }

    @Test
    fun givenFirstOptionFocused_whenMovingPrevious_thenFocusWrapsToLastOption() {
        assertEquals(3, richTextFocusTargetIndex(0, 4, RichTextFocusMove.Previous))
    }

    @Test
    fun givenMiddleOptionFocused_whenMovingInEitherDirection_thenFocusMovesToAdjacentOption() {
        assertEquals(2, richTextFocusTargetIndex(1, 4, RichTextFocusMove.Next))
        assertEquals(0, richTextFocusTargetIndex(1, 4, RichTextFocusMove.Previous))
    }
}
