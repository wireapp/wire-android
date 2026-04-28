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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.conversation

import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi
import com.wire.kalium.logic.data.id.ConversationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationFilterSheetStateTest {

    private companion object {
        val conv1 = FilterConversationUi(id = ConversationId("conv1", "domain"), name = "Alpha")
        val conv2 = FilterConversationUi(id = ConversationId("conv2", "domain"), name = "Beta")
    }

    @Test
    fun `given no initial selection, then selectedConversation is null`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        assertNull(state.selectedConversation)
    }

    @Test
    fun `given initial selected conversation, then selectedConversation matches`() {
        val state = ConversationFilterSheetState(initialSelected = conv1)

        assertEquals(conv1, state.selectedConversation)
    }

    @Test
    fun `given no initial selection, then hasChanges is false`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given initial selection, then hasChanges is false`() {
        val state = ConversationFilterSheetState(initialSelected = conv1)

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given no initial selection, when conversation selected, then hasChanges is true`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        state.selectConversation(conv1)

        assertTrue(state.hasChanges)
    }

    @Test
    fun `given initial selection, when different conversation selected, then hasChanges is true`() {
        val state = ConversationFilterSheetState(initialSelected = conv1)

        state.selectConversation(conv2)

        assertTrue(state.hasChanges)
    }

    @Test
    fun `given initial selection, when same conversation selected again, then it is deselected and hasChanges is true`() {
        val state = ConversationFilterSheetState(initialSelected = conv1)

        state.selectConversation(conv1)

        assertNull(state.selectedConversation)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `given no initial selection, when conversation selected then deselected, then hasChanges is false`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        state.selectConversation(conv1)
        state.selectConversation(conv1) // toggle off

        assertNull(state.selectedConversation)
        assertFalse(state.hasChanges)
    }

    @Test
    fun `given conversation A selected, when conversation B selected, then only B is the selection`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        state.selectConversation(conv1)
        state.selectConversation(conv2)

        assertEquals(conv2, state.selectedConversation)
    }

    @Test
    fun `given conversation selected, when removeAll called, then selectedConversation is null`() {
        val state = ConversationFilterSheetState(initialSelected = null)
        state.selectConversation(conv1)

        state.removeAll()

        assertNull(state.selectedConversation)
    }

    @Test
    fun `given initial selection, when removeAll called, then hasChanges is true`() {
        val state = ConversationFilterSheetState(initialSelected = conv1)

        state.removeAll()

        assertNull(state.selectedConversation)
        assertTrue(state.hasChanges)
    }

    @Test
    fun `given no initial selection, when removeAll called, then hasChanges is false`() {
        val state = ConversationFilterSheetState(initialSelected = null)

        state.removeAll()

        assertFalse(state.hasChanges)
    }
}
