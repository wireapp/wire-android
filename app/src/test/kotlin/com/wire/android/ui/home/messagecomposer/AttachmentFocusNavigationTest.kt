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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AttachmentFocusNavigationTest {

    @Test
    fun givenLinearNavigation_whenMovingForwardAndBackward_thenTargetsAreSymmetric() {
        assertEquals(3, attachmentFocusTargetIndex(2, 6, 5, AttachmentFocusMove.Next))
        assertEquals(2, attachmentFocusTargetIndex(3, 6, 5, AttachmentFocusMove.Previous))
        assertEquals(5, attachmentFocusTargetIndex(0, 6, 5, AttachmentFocusMove.Previous))
        assertEquals(0, attachmentFocusTargetIndex(5, 6, 5, AttachmentFocusMove.Next))
    }

    @Test
    fun givenGridNavigation_whenMovingHorizontally_thenFocusStaysInTheCurrentRow() {
        assertEquals(1, attachmentFocusTargetIndex(0, 6, 5, AttachmentFocusMove.Right))
        assertEquals(3, attachmentFocusTargetIndex(4, 6, 5, AttachmentFocusMove.Left))
        assertNull(attachmentFocusTargetIndex(4, 6, 5, AttachmentFocusMove.Right))
        assertNull(attachmentFocusTargetIndex(5, 6, 5, AttachmentFocusMove.Left))
    }

    @Test
    fun givenGridNavigation_whenMovingVertically_thenFocusUsesTheColumnCount() {
        assertEquals(5, attachmentFocusTargetIndex(0, 6, 5, AttachmentFocusMove.Down))
        assertEquals(0, attachmentFocusTargetIndex(5, 6, 5, AttachmentFocusMove.Up))
        assertNull(attachmentFocusTargetIndex(1, 6, 5, AttachmentFocusMove.Down))
    }

    @Test
    fun givenDisabledOptions_whenMovingLinearly_thenFocusSkipsThem() {
        val enabledOptions = listOf(true, false, false, true)

        assertEquals(3, attachmentFocusableTargetIndex(0, enabledOptions, 4, AttachmentFocusMove.Next))
        assertEquals(0, attachmentFocusableTargetIndex(3, enabledOptions, 4, AttachmentFocusMove.Previous))
    }
}
