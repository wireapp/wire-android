/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.details.metadata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EditGroupNameValidatorTest {

    @Test
    fun `given group name is empty, when validating, then empty error is exposed`() {
        val state = EditGroupNameValidator.onGroupNameChange(" ", EditConversationMetadataState())

        assertFalse(state.continueEnabled)
        assertTrue(state.animatedGroupNameError)
        assertEquals(EditConversationMetadataState.NameError.Empty, state.error)
    }

    @Test
    fun `given group name exceeds limit, when validating, then limit error is exposed`() {
        val state = EditGroupNameValidator.onGroupNameChange("a".repeat(65), EditConversationMetadataState())

        assertFalse(state.continueEnabled)
        assertTrue(state.animatedGroupNameError)
        assertEquals(EditConversationMetadataState.NameError.TooLong, state.error)
    }

    @Test
    fun `given group name is unchanged after trimming, when validating, then continue is disabled without error`() {
        val state = EditGroupNameValidator.onGroupNameChange(
            " group name ",
            EditConversationMetadataState(originalGroupName = "group name"),
        )

        assertFalse(state.continueEnabled)
        assertFalse(state.animatedGroupNameError)
        assertEquals(EditConversationMetadataState.NameError.None, state.error)
    }

    @Test
    fun `given changed group name, when validating, then continue is enabled without error`() {
        val state = EditGroupNameValidator.onGroupNameChange("new group name", EditConversationMetadataState())

        assertTrue(state.continueEnabled)
        assertFalse(state.animatedGroupNameError)
        assertEquals(EditConversationMetadataState.NameError.None, state.error)
    }

    @Test
    fun `given error animation completed, when adapting, then only animation flag is cleared`() {
        val currentState = EditConversationMetadataState(
            originalGroupName = "group name",
            animatedGroupNameError = true,
            error = EditConversationMetadataState.NameError.Empty,
            completed = EditConversationMetadataState.Completed.Failure,
        )

        val state = EditGroupNameValidator.onGroupNameErrorAnimated(currentState)

        assertEquals(currentState.copy(animatedGroupNameError = false), state)
    }
}
