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
import org.junit.jupiter.api.Test

class GroupNameValidatorTest {

    @Test
    fun `given group name has leading spaces, when validating, then continue is disabled`() {
        val state = GroupNameValidator.onGroupNameChange(" group name", GroupMetadataState())

        assertFalse(state.continueEnabled)
        assertEquals(
            GroupMetadataState.NewGroupError.TextFieldError.GroupNameLeadingTrailingSpacesError,
            state.error
        )
    }

    @Test
    fun `given group name has trailing spaces, when validating, then continue is disabled`() {
        val state = GroupNameValidator.onGroupNameChange("group name ", GroupMetadataState())

        assertFalse(state.continueEnabled)
        assertEquals(
            GroupMetadataState.NewGroupError.TextFieldError.GroupNameLeadingTrailingSpacesError,
            state.error
        )
    }
}
