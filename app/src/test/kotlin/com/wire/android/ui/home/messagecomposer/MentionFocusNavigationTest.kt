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

import androidx.compose.ui.input.key.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MentionFocusNavigationTest {

    @Test
    fun givenLastMemberFocused_whenMovingNext_thenFocusWrapsToFirstMember() {
        assertEquals(0, mentionFocusTargetIndex(3, 4, MentionFocusMove.Next))
    }

    @Test
    fun givenFirstMemberFocused_whenMovingPrevious_thenFocusWrapsToLastMember() {
        assertEquals(3, mentionFocusTargetIndex(0, 4, MentionFocusMove.Previous))
    }

    @Test
    fun givenMiddleMemberFocused_whenMovingInEitherDirection_thenFocusMovesToAdjacentMember() {
        assertEquals(2, mentionFocusTargetIndex(1, 4, MentionFocusMove.Next))
        assertEquals(0, mentionFocusTargetIndex(1, 4, MentionFocusMove.Previous))
    }

    @Test
    fun givenEscapePressed_whenMappingKeyAction_thenDismissIsReturned() {
        assertEquals(MentionKeyAction.Dismiss, mentionKeyAction(Key.Escape, false))
    }

    @Test
    fun givenTabPressed_whenMappingKeyAction_thenDirectionMatchesShiftState() {
        assertEquals(MentionKeyAction.Next, mentionKeyAction(Key.Tab, false))
        assertEquals(MentionKeyAction.Previous, mentionKeyAction(Key.Tab, true))
    }

    @Test
    fun givenReversedList_whenMappingArrowKeys_thenDirectionMatchesVisualOrder() {
        assertEquals(MentionKeyAction.Next, mentionKeyAction(Key.DirectionUp, false, reverseLayout = true))
        assertEquals(MentionKeyAction.Previous, mentionKeyAction(Key.DirectionDown, false, reverseLayout = true))
    }

    @Test
    fun givenActivationKeyPressed_whenMappingKeyAction_thenPickIsReturned() {
        assertEquals(MentionKeyAction.Pick, mentionKeyAction(Key.Enter, false))
        assertEquals(MentionKeyAction.Pick, mentionKeyAction(Key.Spacebar, false))
    }
}
