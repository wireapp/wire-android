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
package com.wire.android.feature.cells.ui.search.filter.bottomsheet.tags

import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TagsFilterSheetStateTest {

    private companion object {
        val testTags = listOf(
            FilterTagUi(id = "1", name = "Important", selected = false),
            FilterTagUi(id = "2", name = "Work", selected = false),
            FilterTagUi(id = "3", name = "Personal", selected = false),
            FilterTagUi(id = "4", name = "Archived", selected = false),
        )
    }

    @Test
    fun `given initial state, then tags are set correctly`() {
        val state = TagsFilterSheetState(testTags)

        assertEquals(4, state.tags.size)
        assertEquals(testTags, state.tags)
    }

    @Test
    fun `given initial state, then hasChanges is false`() {
        val state = TagsFilterSheetState(testTags)

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given tag toggled, then hasChanges is true`() {
        val state = TagsFilterSheetState(testTags)

        state.toggle("1")

        assertTrue(state.hasChanges)
    }

    @Test
    fun `given tag toggled twice, then hasChanges is false`() {
        val state = TagsFilterSheetState(testTags)

        state.toggle("1")
        state.toggle("1")

        assertFalse(state.hasChanges)
    }

    @Test
    fun `given toggle called, then tag selection is toggled`() {
        val state = TagsFilterSheetState(testTags)

        state.toggle("1")

        assertTrue(state.tags.first { it.id == "1" }.selected)
    }

    @Test
    fun `given toggle called on selected tag, then tag is deselected`() {
        val initialTags = listOf(
            FilterTagUi(id = "1", name = "Important", selected = true),
            FilterTagUi(id = "2", name = "Work", selected = false),
        )
        val state = TagsFilterSheetState(initialTags)

        state.toggle("1")

        assertFalse(state.tags.first { it.id == "1" }.selected)
    }

    @Test
    fun `given multiple tags selected, when removeAll called, then all tags are deselected`() {
        val state = TagsFilterSheetState(testTags)
        state.toggle("1")
        state.toggle("2")

        state.removeAll()

        assertFalse(state.tags.any { it.selected })
    }

    @Test
    fun `given tags selected, then selectedTags returns only selected tags`() {
        val state = TagsFilterSheetState(testTags)
        state.toggle("1")
        state.toggle("3")

        val selected = state.selectedTags()

        assertEquals(2, selected.size)
        assertTrue(selected.any { it.id == "1" })
        assertTrue(selected.any { it.id == "3" })
    }

    @Test
    fun `given no tags selected, then selectedTags returns empty list`() {
        val state = TagsFilterSheetState(testTags)

        val selected = state.selectedTags()

        assertTrue(selected.isEmpty())
    }

    @Test
    fun `given empty query, then filteredTags returns all tags`() {
        val state = TagsFilterSheetState(testTags)

        val filtered = state.filteredTags("")

        assertEquals(4, filtered.size)
    }

    @Test
    fun `given blank query, then filteredTags returns all tags`() {
        val state = TagsFilterSheetState(testTags)

        val filtered = state.filteredTags("   ")

        assertEquals(4, filtered.size)
    }

    @Test
    fun `given query matching one tag, then filteredTags returns matching tag`() {
        val state = TagsFilterSheetState(testTags)

        val filtered = state.filteredTags("Import")

        assertEquals(1, filtered.size)
        assertEquals("Important", filtered.first().name)
    }

    @Test
    fun `given query matching multiple tags, then filteredTags returns all matching tags`() {
        val tags = listOf(
            FilterTagUi(id = "1", name = "Work Project", selected = false),
            FilterTagUi(id = "2", name = "Work Meeting", selected = false),
            FilterTagUi(id = "3", name = "Personal", selected = false),
        )
        val state = TagsFilterSheetState(tags)

        val filtered = state.filteredTags("Work")

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.name.contains("Work") })
    }

    @Test
    fun `given query with different case, then filteredTags is case insensitive`() {
        val state = TagsFilterSheetState(testTags)

        val filtered = state.filteredTags("IMPORTANT")

        assertEquals(1, filtered.size)
        assertEquals("Important", filtered.first().name)
    }

    @Test
    fun `given query matching no tags, then filteredTags returns empty list`() {
        val state = TagsFilterSheetState(testTags)

        val filtered = state.filteredTags("NonExistent")

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `given filtered tags, then selected tags appear first`() {
        val state = TagsFilterSheetState(testTags)
        state.toggle("3") // Select "Personal"

        val filtered = state.filteredTags("")

        assertEquals("Personal", filtered.first().name)
        assertTrue(filtered.first().selected)
    }

    @Test
    fun `given filtered tags with same selection status, then sorted alphabetically`() {
        val tags = listOf(
            FilterTagUi(id = "1", name = "Zebra", selected = false),
            FilterTagUi(id = "2", name = "Apple", selected = false),
            FilterTagUi(id = "3", name = "Mango", selected = false),
        )
        val state = TagsFilterSheetState(tags)

        val filtered = state.filteredTags("")

        assertEquals("Apple", filtered[0].name)
        assertEquals("Mango", filtered[1].name)
        assertEquals("Zebra", filtered[2].name)
    }

    @Test
    fun `given mixed selected and unselected tags, then sorted by selected first then alphabetically`() {
        val tags = listOf(
            FilterTagUi(id = "1", name = "Zebra", selected = false),
            FilterTagUi(id = "2", name = "Apple", selected = false),
            FilterTagUi(id = "3", name = "Mango", selected = true),
            FilterTagUi(id = "4", name = "Banana", selected = true),
        )
        val state = TagsFilterSheetState(tags)

        val filtered = state.filteredTags("")

        assertEquals("Banana", filtered[0].name)
        assertEquals("Mango", filtered[1].name)
        assertEquals("Apple", filtered[2].name)
        assertEquals("Zebra", filtered[3].name)
    }
}

