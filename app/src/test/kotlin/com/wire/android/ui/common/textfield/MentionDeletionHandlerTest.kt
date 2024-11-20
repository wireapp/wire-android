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
package com.wire.android.ui.common.textfield

import androidx.compose.ui.text.TextRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MentionDeletionHandlerTest {

    @Test
    fun `given mention in text when deleting inside mention then mention is removed`() {
        val oldText = "Hello @John Doe, how are you?"
        val newText = "Hello , how are you?"
        val oldSelection = TextRange(6, 17)
        val mentions = listOf("@John Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals("Hello , how are you?", result)
    }

    @Test
    fun `given mention with last character deleted when deleting last character then mention is removed`() {
        val oldText = "Hello @John Doe, how are you?"
        val newText = "Hello @John Do, how are you?"
        val oldSelection = TextRange(3, 13)
        val mentions = listOf("@John Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals("Hello , how are you?", result)
    }

    @Test
    fun `given cursor at beginning of mention when no deletion then text remains unchanged`() {
        val oldText = "Hello @John Doe, how are you?"
        val newText = "Hello @John Doe, how are you?"
        val oldSelection = TextRange(6, 6)
        val mentions = listOf("@John Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals(oldText, result)
    }

    @Test
    fun `given text with mention when deleting outside of mention then text remains unchanged`() {
        val oldText = "Hello @John Doe, how are you?"
        val newText = "Hello @John Doehow are you?"
        val oldSelection = TextRange(5, 6)
        val mentions = listOf("@John Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals(newText, result)
    }

    @Test
    fun `given multiple mentions in text when deleting inside mentions then all mentions are removed`() {
        val oldText = "Hello @John Doe and @Jane Doe, how are you?"
        val newText = "Hello , how are you?"
        val oldSelection = TextRange(6, 17)
        val mentions = listOf("@John Doe", "@Jane Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals(newText, result)
    }

    @Test
    fun `given text without mentions when no mentions to delete then text remains unchanged`() {
        val oldText = "Hello there, how are you?"
        val newText = "Hello, how are you?"
        val oldSelection = TextRange(6, 6)
        val mentions = listOf("@John Doe")

        val result = MentionDeletionHandler.handle(oldText, newText, oldSelection, mentions)

        assertEquals(newText, result)
    }
}
