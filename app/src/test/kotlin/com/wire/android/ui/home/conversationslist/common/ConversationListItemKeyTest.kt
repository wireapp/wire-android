/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.conversationslist.common

import com.wire.android.ui.home.conversationslist.model.ConversationSection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ConversationListItemKeyTest {

    @Test
    fun givenDuplicateConversationSeparators_whenCreatingKeys_thenEachPositionIsUnique() {
        val separator = ConversationSection.Predefined.Conversations

        val first = conversationListItemKey(separator, index = 3)
        val duplicate = conversationListItemKey(separator, index = 8)

        assertNotEquals(first, duplicate)
    }

    @Test
    fun givenSameSeparatorAtSamePosition_whenCreatingKeyAgain_thenKeyIsStable() {
        val separator = ConversationSection.Predefined.Conversations

        assertEquals(
            conversationListItemKey(separator, index = 3),
            conversationListItemKey(separator, index = 3),
        )
    }

    @Test
    fun givenPagingPlaceholders_whenCreatingKeys_thenEachPositionIsUnique() {
        assertNotEquals(
            conversationListItemKey(item = null, index = 3),
            conversationListItemKey(item = null, index = 8),
        )
    }
}
