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
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.util.EMPTY
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentionUpdateCoordinatorTest {

    @MockK
    private lateinit var mentionAdjuster: MentionAdjuster

    @MockK
    private lateinit var selectionManager: MentionSelectionManager

    init {
        MockKAnnotations.init(this)
    }

    private val coordinator = MentionUpdateCoordinator(
        mentionAdjuster = mentionAdjuster,
        selectionManager = selectionManager
    )

    @Test
    fun `Given empty new text, When handle is called, Then mentions should be cleared`() {
        // Given
        val oldTextFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val newTextFieldValue = TextFieldValue(text = "", selection = TextRange(0, 0))
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))
        var isInvoked = false
        // When
        val updatedTextFieldValue = coordinator.handle(
            oldTextFieldValue,
            newTextFieldValue,
            mentions,
            updateMentions = { isInvoked = true }
        )

        // Then
        assertEquals(String.EMPTY, updatedTextFieldValue.text)
        assertTrue(isInvoked)
    }

    @Test
    fun `Given no mention change, When handle is called, Then mentions should remain unchanged`() {
        // Given
        val oldTextFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val newTextFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val mentions = listOf(UIMention(start = 0, length = 5, handler = "@user", userId = TestUser.USER_ID))
        var isInvoked = false
        every {
            selectionManager.updateSelectionForMention(any(), any(), any())
        } returns TextRange(0, 5)

        // When
        val updatedTextFieldValue = coordinator.handle(
            oldTextFieldValue,
            newTextFieldValue,
            mentions,
            updateMentions = { isInvoked = true }
        )

        // Then
        assertEquals("Hello", updatedTextFieldValue.text)
        assertEquals(TextRange(0, 5), updatedTextFieldValue.selection)
        assertFalse(isInvoked)
    }

    @Test
    fun `Given text deletion, When handle is called, Then mentions and selection should adjust`() {
        // Given
        val oldTextFieldValue = TextFieldValue(text = "Hello @user", selection = TextRange(0, 11))
        val newTextFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val mentions = listOf(UIMention(start = 6, length = 5, handler = "@user", userId = TestUser.USER_ID))
        var isInvoked = false

        every {
            mentionAdjuster.adjustMentionsForDeletion(
                mentions = mentions,
                deletedLength = 6,
                text = "Hello",
                selection = newTextFieldValue.selection
            )
        } returns Pair(listOf(UIMention(start = 6, length = 5, handler = "@user", userId = TestUser.USER_ID)), TextRange(0, 5))

        // When
        val updatedTextFieldValue = coordinator.handle(
            oldTextFieldValue,
            newTextFieldValue,
            mentions,
            updateMentions = { isInvoked = true }
        )

        // Then
        assertEquals("Hello", updatedTextFieldValue.text)
        assertEquals(TextRange(0, 5), updatedTextFieldValue.selection)
        assertTrue(isInvoked)
    }

    @Test
    fun `Given text insertion, When handle is called, Then mentions should shift accordingly`() {
        // Given
        val oldTextFieldValue = TextFieldValue(text = "Hello", selection = TextRange(0, 5))
        val newTextFieldValue = TextFieldValue(text = "Hello @user", selection = TextRange(0, 5))
        val mentions = listOf(UIMention(start = 6, length = 5, handler = "@user", userId = TestUser.USER_ID))
        var isInvoked = false

        every {
            mentionAdjuster.adjustMentionsForInsertion(
                mentions = mentions,
                addedLength = 6,
                text = any(),
                selection = any()
            )
        } returns Pair(listOf(UIMention(start = 12, length = 5, handler = "@user", userId = TestUser.USER_ID)), TextRange(0, 5))

        // When
        val updatedTextFieldValue = coordinator.handle(
            oldTextFieldValue,
            newTextFieldValue,
            mentions,
            updateMentions = { isInvoked = true }
        )

        // Then
        assertEquals("Hello @user", updatedTextFieldValue.text)
        assertEquals(TextRange(0, 5), updatedTextFieldValue.selection)
        assertTrue(isInvoked)
    }

    @Test
    fun `Given selection inside mention, When handle is called, Then selection should update`() {
        // Given
        val oldTextFieldValue = TextFieldValue(text = "Hello @user", selection = TextRange(0, 5))
        val newTextFieldValue = TextFieldValue(text = "Hello @user", selection = TextRange(0, 5))
        val mentions = listOf(UIMention(start = 6, length = 5, handler = "@user", userId = TestUser.USER_ID))
        var isInvoked = false

        every {
            selectionManager.updateSelectionForMention(any(), any(), any())
        } returns TextRange(6, 11)

        // When
        val updatedTextFieldValue = coordinator.handle(
            oldTextFieldValue,
            newTextFieldValue,
            mentions,
            updateMentions = { isInvoked = true }
        )

        // Then
        assertEquals(TextRange(6, 11), updatedTextFieldValue.selection)
        assertFalse(isInvoked)
    }
}
