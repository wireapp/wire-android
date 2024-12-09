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

class MentionAdjusterTest {

    private val mentionAdjuster: MentionAdjuster = MentionAdjuster()

    // --- adjustMentionsForDeletion Tests ---

    @Test
    fun `Given deleted text does not affect any mentions, When adjustMentionsForDeletion is called, Then mentions remain unchanged`() {
        // Given
        val mentions = listOf(UIMention(start = 6, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val deletedLength = 2
        val text = "Hello @user again"
        val selection = TextRange(13, 10)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForDeletion(
            mentions = mentions,
            deletedLength = deletedLength,
            text = text,
            selection = selection
        )

        // Then
        assertEquals(mentions, updatedMentions)
        assertEquals(selection, updatedSelection)
    }

    @Test
    fun `Given deleted text affects mentions, When adjustMentionsForDeletion is called, Then mentions are adjusted and selection updated`() {
        // Given
        val mentions = listOf(UIMention(start = 12, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val deletedLength = 2 // Simulate deleting 2 characters before the mention
        val text = "Hello ain @user"
        val selection = TextRange(7, 7)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForDeletion(
            mentions = mentions,
            deletedLength = deletedLength,
            text = text,
            selection = selection
        )

        // Then
        val expectedMention = UIMention(start = 10, length = 5, handler = "@user", userId = TestUser.USER_ID)
        assertEquals(listOf(expectedMention), updatedMentions)
        assertEquals(selection, updatedSelection)
    }

    @Test
    fun `Given cursor is at end of a mention, When adjustMentionsForDeletion is called, Then selection is updated to the mention's range`() {
        // Given
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val deletedLength = 1 // Simulate deleting 1 character inside the mention
        val text = "@user"
        val selection = TextRange(5, 5)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForDeletion(
            mentions = mentions,
            deletedLength = deletedLength,
            text = text,
            selection = selection
        )

        // Then
        val expectedMention = UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID)
        assertEquals(listOf(expectedMention), updatedMentions)
        assertEquals(TextRange(0, 5), updatedSelection)
    }

    // --- adjustMentionsForInsertion Tests ---

    @Test
    fun `Given inserted text does not affect any mentions, When adjustMentionsForInsertion is called, Then mentions remain unchanged`() {
        // Given
        val mentions = listOf(UIMention(start = 5, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val addedLength = 0
        val text = "Hello world"
        val selection = TextRange(0, 5)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForInsertion(
            mentions = mentions,
            text = text,
            selection = selection,
            addedLength = addedLength
        )

        // Then
        assertEquals(mentions, updatedMentions)
        assertEquals(selection, updatedSelection)
    }

    @Test
    fun `Given inserted text shifts mentions, When adjustMentionsForInsertion is called, Then mentions are adjusted`() {
        // Given
        val mentions = listOf(UIMention(start = 5, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val addedLength = 2 // Simulate inserting 2 characters before the mention
        val text = "Hello @user"
        val selection = TextRange(0, 5)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForInsertion(
            mentions = mentions,
            text = text,
            selection = selection,
            addedLength = addedLength
        )

        // Then
        val expectedMention = UIMention(start = 7, length = 5, handler = "@user", userId = TestUser.USER_ID)
        assertEquals(listOf(expectedMention), updatedMentions)
        assertEquals(selection, updatedSelection)
    }

    @Test
    fun `Given inserted text shifts mentions, When adjustMentionsForInsertion is called, Then mentions are adjusted accordingly`() {
        // Given
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))
        val addedLength = 3 // Simulate inserting 3 characters before the mention
        val text = "Hel world"
        val selection = TextRange(0, 5)

        // When
        val (updatedMentions, updatedSelection) = mentionAdjuster.adjustMentionsForInsertion(
            mentions = mentions,
            text = text,
            selection = selection,
            addedLength = addedLength
        )

        // Then
        val expectedMention = UIMention(start = 3, length = 5, handler = "@user", userId = TestUser.USER_ID)
        assertEquals(listOf(expectedMention), updatedMentions)
        assertEquals(selection, updatedSelection)
    }
}
