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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationFilterSheetStateTest {

    private companion object {
        val conv1Id = ConversationId("conv1", "domain")
        val conv2Id = ConversationId("conv2", "domain")
        val conv3Id = ConversationId("conv3", "domain")
        val conv4Id = ConversationId("conv4", "domain")

        val testConversations = listOf(
            FilterConversationUi(
                id = conv1Id,
                name = "Project Alpha",
                selected = false,
                isChannel = false
            ),
            FilterConversationUi(
                id = conv2Id,
                name = "Team Chat",
                selected = false,
                isChannel = false
            ),
            FilterConversationUi(
                id = conv3Id,
                name = "General Channel",
                selected = false,
                isChannel = true
            ),
            FilterConversationUi(
                id = conv4Id,
                name = "Support Group",
                selected = false,
                isChannel = false
            ),
        )
    }

    @Test
    fun `given initial state, then conversations are set correctly`() {
        val state = ConversationFilterSheetState(testConversations)

        assertEquals(4, state.conversations.size)
        assertEquals(testConversations, state.conversations)
    }

    @Test
    fun `given initial state, then hasChanges is false`() {
        val state = ConversationFilterSheetState(testConversations)

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given conversation selected, then hasChanges is true`() {
        val state = ConversationFilterSheetState(testConversations)

        state.selectConversation(conv1Id.toString())

        assertTrue(state.hasChanges)
    }

    @Test
    fun `given conversation selected twice, then hasChanges is false`() {
        val state = ConversationFilterSheetState(testConversations)

        state.selectConversation(conv1Id.toString())
        state.selectConversation(conv1Id.toString())

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given selectConversation called, then conversation is selected`() {
        val state = ConversationFilterSheetState(testConversations)

        state.selectConversation(conv1Id.toString())

        assertTrue(state.conversations.first { it.id == conv1Id }.selected)
    }

    @Test
    fun `given selectConversation called, then other conversations are deselected`() {
        val initialConversations = listOf(
            FilterConversationUi(
                id = conv1Id,
                name = "Project Alpha",
                selected = true,
                isChannel = false
            ),
            FilterConversationUi(
                id = conv2Id,
                name = "Team Chat",
                selected = false,
                isChannel = false
            ),
        )
        val state = ConversationFilterSheetState(initialConversations)

        state.selectConversation(conv2Id.toString())

        assertFalse(state.conversations.first { it.id == conv1Id }.selected)
        assertTrue(state.conversations.first { it.id == conv2Id }.selected)
    }

    @Test
    fun `given selectConversation called on selected conversation, then conversation is deselected`() {
        val initialConversations = listOf(
            FilterConversationUi(
                id = conv1Id,
                name = "Project Alpha",
                selected = true,
                isChannel = false
            ),
        )
        val state = ConversationFilterSheetState(initialConversations)

        state.selectConversation(conv1Id.toString())

        assertFalse(state.conversations.first { it.id == conv1Id }.selected)
    }

    @Test
    fun `given conversations selected, when removeAll called, then all conversations are deselected`() {
        val state = ConversationFilterSheetState(testConversations)
        state.selectConversation(conv1Id.toString())

        state.removeAll()

        assertFalse(state.conversations.any { it.selected })
    }

    @Test
    fun `given conversation selected, then selectedConversation returns only selected conversation`() {
        val state = ConversationFilterSheetState(testConversations)
        state.selectConversation(conv1Id.toString())

        val selected = state.selectedConversation()

        assertEquals(1, selected.size)
        assertEquals(conv1Id, selected.first().id)
    }

    @Test
    fun `given no conversation selected, then selectedConversation returns empty list`() {
        val state = ConversationFilterSheetState(testConversations)

        val selected = state.selectedConversation()

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `given empty query, then filteredConversations returns all conversations`() {
        val state = ConversationFilterSheetState(testConversations)

        assertEquals(4, state.filteredConversations("").size)
    }

    @Test
    fun `given blank query, then filteredConversations returns all conversations`() {
        val state = ConversationFilterSheetState(testConversations)

        assertEquals(4, state.filteredConversations("   ").size)
    }

    @Test
    fun `given query matching one conversation, then filteredConversations returns matching conversation`() {
        val state = ConversationFilterSheetState(testConversations)

        val filtered = state.filteredConversations("Alpha")

        assertEquals(1, filtered.size)
        assertEquals("Project Alpha", filtered.first().name)
    }

    @Test
    fun `given query matching multiple conversations, then filteredConversations returns all matching`() {
        val conversations = listOf(
            FilterConversationUi(
                id = ConversationId("conv1", "domain"),
                name = "Team Alpha",
                selected = false,
                isChannel = false
            ),
            FilterConversationUi(
                id = ConversationId("conv2", "domain"),
                name = "Team Beta",
                selected = false,
                isChannel = false
            ),
            FilterConversationUi(
                id = ConversationId("conv3", "domain"),
                name = "Project Gamma",
                selected = false,
                isChannel = false
            ),
        )
        val state = ConversationFilterSheetState(conversations)

        val filtered = state.filteredConversations("Team")

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.name.contains("Team") })
    }

    @Test
    fun `given query with different case, then filteredConversations is case insensitive`() {
        val state = ConversationFilterSheetState(testConversations)

        val filtered = state.filteredConversations("PROJECT ALPHA")

        assertEquals(1, filtered.size)
        assertEquals("Project Alpha", filtered.first().name)
    }

    @Test
    fun `given query matching no conversations, then filteredConversations returns empty list`() {
        val state = ConversationFilterSheetState(testConversations)

        val filtered = state.filteredConversations("NonExistent")

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `given query matching partial name, then filteredConversations returns matching conversations`() {
        val state = ConversationFilterSheetState(testConversations)

        val filtered = state.filteredConversations("Chan")

        assertEquals(1, filtered.size)
        assertEquals("General Channel", filtered.first().name)
    }

    @Test
    fun `given empty conversations list, then state handles empty list correctly`() {
        val state = ConversationFilterSheetState(emptyList())

        assertEquals(0, state.conversations.size)
        assertFalse(state.hasChanges)
        assertTrue(state.selectedConversation().isEmpty())
        assertTrue(state.filteredConversations("test").isEmpty())
    }

    @Test
    fun `given single selection mode, only one conversation can be selected at a time`() {
        val state = ConversationFilterSheetState(testConversations)

        state.selectConversation(conv1Id.toString())
        state.selectConversation(conv2Id.toString())

        val selected = state.selectedConversation()
        assertEquals(1, selected.size)
        assertEquals(conv2Id, selected.first().id)
    }
}

