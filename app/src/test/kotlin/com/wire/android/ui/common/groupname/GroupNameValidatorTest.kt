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

package com.wire.android.ui.common.groupname

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupNameValidatorTest {

    @Test
    fun `given group name has leading spaces, when validating, then continue is enabled`() {
        val state = GroupNameValidator.onGroupNameChange(" group name", GroupMetadataState())

        assertTrue(state.continueEnabled)
        assertEquals(GroupMetadataState.NewGroupError.None, state.error)
    }

    @Test
    fun `given group name has trailing spaces, when validating, then continue is enabled`() {
        val state = GroupNameValidator.onGroupNameChange("group name ", GroupMetadataState())

        assertTrue(state.continueEnabled)
        assertEquals(GroupMetadataState.NewGroupError.None, state.error)
    }

    @Test
    fun `given group name is empty, when validating, then adapter exposes empty error`() {
        val state = GroupNameValidator.onGroupNameChange(" ", GroupMetadataState())

        assertFalse(state.continueEnabled)
        assertTrue(state.animatedGroupNameError)
        assertEquals(GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError, state.error)
    }

    @Test
    fun `given group name is unchanged after trimming, when validating, then adapter disables continue without error`() {
        val state = GroupNameValidator.onGroupNameChange(" group name ", GroupMetadataState(originalGroupName = "group name"))

        assertFalse(state.continueEnabled)
        assertFalse(state.animatedGroupNameError)
        assertEquals(GroupMetadataState.NewGroupError.None, state.error)
    }

    @Test
    fun `given group name exceeds limit, when validating, then adapter exposes limit error`() {
        val state = GroupNameValidator.onGroupNameChange("a".repeat(65), GroupMetadataState())

        assertFalse(state.continueEnabled)
        assertTrue(state.animatedGroupNameError)
        assertEquals(GroupMetadataState.NewGroupError.TextFieldError.GroupNameExceedLimitError, state.error)
    }

    @Test
    fun `given error animation completed, when adapting, then only animation flag is cleared`() {
        val currentState = GroupMetadataState(
            animatedGroupNameError = true,
            error = GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError
        )

        val state = GroupNameValidator.onGroupNameErrorAnimated(currentState)

        assertFalse(state.animatedGroupNameError)
        assertEquals(currentState.error, state.error)
    }
}
