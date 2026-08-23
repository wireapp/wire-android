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

package com.wire.android.ui.home.conversations.details.participants

import com.wire.android.ui.home.conversations.details.participants.model.ParticipantsExpansionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupConversationParticipantsStateTest {

    @Test
    fun previewProvidesOneAdminAndOneParticipant() {
        val preview = GroupConversationParticipantsState.PREVIEW.data

        assertEquals(1, preview.admins.size)
        assertEquals(1, preview.participants.size)
        assertTrue(preview.isSelfAnAdmin)
    }

    @Test
    fun expansionStateChangesEachSectionIndependently() {
        val state = ParticipantsExpansionState()

        state.membersActions.onExpansionChanged(false)
        state.appsActions.onExpansionChanged(false)

        assertFalse(state.membersActions.expanded.value)
        assertTrue(state.adminsActions.expanded.value)
        assertFalse(state.appsActions.expanded.value)
    }
}
