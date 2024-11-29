/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.common.textfield.mention

import androidx.compose.ui.text.TextRange
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.UIMention
import org.junit.Assert.assertEquals
import org.junit.Test

class MentionSelectionManagerTest {

    private val selectionManager: MentionSelectionManager = MentionSelectionManager()

    @Test
    fun `Given same old and new selection, When updateSelectionForMention is called, Then selection remains unchanged`() {
        // Given
        val oldSelection = TextRange(0, 5)
        val newSelection = TextRange(0, 5) // Same as old selection
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))

        // When
        val updatedSelection = selectionManager.updateSelectionForMention(
            oldSelection = oldSelection,
            newSelection = newSelection,
            mentions = mentions
        )

        // Then
        assertEquals(newSelection, updatedSelection)
    }

    @Test
    fun `Given new selection inside a mention, When updateSelectionForMention is called, Then selection updates to the mention's range`() {
        // Given
        val oldSelection = TextRange(0, 5)
        val newSelection = TextRange(3, 3) // Inside the mention range
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))

        // When
        val updatedSelection = selectionManager.updateSelectionForMention(
            oldSelection = oldSelection,
            newSelection = newSelection,
            mentions = mentions
        )

        // Then
        assertEquals(TextRange(0, 5), updatedSelection)
    }

    @Test
    fun `Given new selection outside of any mention, When updateSelectionForMention is called, Then selection remains unchanged`() {
        // Given
        val oldSelection = TextRange(0, 5)
        val newSelection = TextRange(10, 10)  // Outside the mention range
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))

        // When
        val updatedSelection = selectionManager.updateSelectionForMention(
            oldSelection = oldSelection,
            newSelection = newSelection,
            mentions = mentions
        )

        // Then
        assertEquals(newSelection, updatedSelection)  // Should remain unchanged
    }

    @Test
    fun `Given multiple mentions, When new selection is inside one of them, Then selection updates to the correct mention's range`() {
        // Given
        val oldSelection = TextRange(0, 5)
        val newSelection = TextRange(8, 8)  // Inside the second mention
        val mentions = listOf(
            UIMention(start = 0, length = 5, handler = "@user1", userId = TestUser.USER_ID),
            UIMention(start = 6, length = 5, handler = "@user2", userId = TestUser.SELF_USER_ID),
            UIMention(start = 15, length = 5, handler = "@user2", userId = TestUser.SELF_USER_ID)
        )

        // When
        val updatedSelection = selectionManager.updateSelectionForMention(
            oldSelection = oldSelection,
            newSelection = newSelection,
            mentions = mentions
        )

        // Then
        assertEquals(TextRange(6, 11), updatedSelection)
    }
}
