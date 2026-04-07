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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.owner

import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OwnersFilterSheetStateTest {

    private companion object {
        val testOwners = listOf(
            FilterOwnerUi(id = "1", displayName = "John Doe", handle = "johndoe", selected = false),
            FilterOwnerUi(id = "2", displayName = "Jane Smith", handle = "janesmith", selected = false),
            FilterOwnerUi(id = "3", displayName = "Bob Wilson", handle = "bobwilson", selected = false),
            FilterOwnerUi(id = "4", displayName = "Alice Brown", handle = "alicebrown", selected = false),
        )
    }

    @Test
    fun `given initial state, then owners are set correctly`() {
        val state = OwnersFilterSheetState(testOwners)

        assertEquals(4, state.owners.size)
        assertEquals(testOwners, state.owners)
    }

    @Test
    fun `given initial state, then hasChanges is false`() {
        val state = OwnersFilterSheetState(testOwners)

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given owner toggled, then hasChanges is true`() {
        val state = OwnersFilterSheetState(testOwners)

        state.toggleOwner("1")

        assertTrue(state.hasChanges)
    }

    @Test
    fun `given owner toggled twice, then hasChanges is false`() {
        val state = OwnersFilterSheetState(testOwners)

        state.toggleOwner("1")
        state.toggleOwner("1")

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given toggleOwner called, then owner selection is toggled`() {
        val state = OwnersFilterSheetState(testOwners)

        state.toggleOwner("1")

        assertTrue(state.owners.first { it.id == "1" }.selected)
    }

    @Test
    fun `given toggleOwner called on selected owner, then owner is deselected`() {
        val initialOwners = listOf(
            FilterOwnerUi(id = "1", displayName = "John Doe", handle = "johndoe", selected = true),
            FilterOwnerUi(id = "2", displayName = "Jane Smith", handle = "janesmith", selected = false),
        )
        val state = OwnersFilterSheetState(initialOwners)

        state.toggleOwner("1")

        assertFalse(state.owners.first { it.id == "1" }.selected)
    }

    @Test
    fun `given multiple owners selected, when removeAll called, then all owners are deselected`() {
        val state = OwnersFilterSheetState(testOwners)
        state.toggleOwner("1")
        state.toggleOwner("2")

        state.removeAll()

        assertFalse(state.owners.any { it.selected })
    }

    @Test
    fun `given owners selected, then selectedOwners returns only selected owners`() {
        val state = OwnersFilterSheetState(testOwners)
        state.toggleOwner("1")
        state.toggleOwner("3")

        val selected = state.selectedOwners()

        assertEquals(2, selected.size)
        assertTrue(selected.any { it.id == "1" })
        assertTrue(selected.any { it.id == "3" })
    }

    @Test
    fun `given no owners selected, then selectedOwners returns empty list`() {
        val state = OwnersFilterSheetState(testOwners)

        val selected = state.selectedOwners()

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `given empty query, then filteredOwners returns all owners`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("")

        assertEquals(4, filtered.size)
    }

    @Test
    fun `given blank query, then filteredOwners returns all owners`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("   ")

        assertEquals(4, filtered.size)
    }

    @Test
    fun `given query matching displayName, then filteredOwners returns matching owner`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("John")

        assertEquals(1, filtered.size)
        assertEquals("John Doe", filtered.first().displayName)
    }

    @Test
    fun `given query matching handle, then filteredOwners returns matching owner`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("janesmith")

        assertEquals(1, filtered.size)
        assertEquals("Jane Smith", filtered.first().displayName)
    }

    @Test
    fun `given query matching multiple owners by displayName, then filteredOwners returns all matching`() {
        val owners = listOf(
            FilterOwnerUi(id = "1", displayName = "John Doe", handle = "johndoe", selected = false),
            FilterOwnerUi(id = "2", displayName = "John Smith", handle = "johnsmith", selected = false),
            FilterOwnerUi(id = "3", displayName = "Jane Doe", handle = "janedoe", selected = false),
        )
        val state = OwnersFilterSheetState(owners)

        val filtered = state.filteredOwners("John")

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.displayName.contains("John") })
    }

    @Test
    fun `given query with different case, then filteredOwners is case insensitive for displayName`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("JOHN DOE")

        assertEquals(1, filtered.size)
        assertEquals("John Doe", filtered.first().displayName)
    }

    @Test
    fun `given query with different case, then filteredOwners is case insensitive for handle`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("JOHNDOE")

        assertEquals(1, filtered.size)
        assertEquals("johndoe", filtered.first().handle)
    }

    @Test
    fun `given query matching no owners, then filteredOwners returns empty list`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("NonExistent")

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `given query matching partial displayName, then filteredOwners returns matching owners`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("Doe")

        assertEquals(1, filtered.size)
        assertEquals("John Doe", filtered.first().displayName)
    }

    @Test
    fun `given query matching partial handle, then filteredOwners returns matching owners`() {
        val state = OwnersFilterSheetState(testOwners)

        val filtered = state.filteredOwners("smith")

        assertEquals(1, filtered.size)
        assertEquals("janesmith", filtered.first().handle)
    }

    @Test
    fun `given empty owners list, then state handles empty list correctly`() {
        val state = OwnersFilterSheetState(emptyList())

        assertEquals(0, state.owners.size)
        assertFalse(state.hasChanges)
        assertTrue(state.selectedOwners().isEmpty())
        assertTrue(state.filteredOwners("test").isEmpty())
    }
}
