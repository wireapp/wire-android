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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi

@Stable
class TagsFilterSheetState(
    initialItems: List<FilterTagUi>
) {
    private val initialById = initialItems.associateBy { it.id }

    var tags by mutableStateOf(initialItems)
        private set

    var query by mutableStateOf("")
        private set

    val hasChanges: Boolean
        get() = tags.any { t -> initialById[t.id]?.selected != t.selected }

    val filteredTags: List<FilterTagUi>
        get() {
            val q = query.trim()
            val base = if (q.isBlank()) tags else tags.filter {
                it.name.contains(q, ignoreCase = true)
            }
            return base.sortedWith(
                compareByDescending<FilterTagUi> { it.selected }
                    .thenBy { it.name.lowercase() }
            )
        }

    fun updateItems(newItems: List<FilterTagUi>) {
        tags = newItems
    }

    fun onQueryChange(text: String) {
        query = text
    }

    fun toggle(id: String) {
        tags = tags.map { if (it.id == id) it.copy(selected = !it.selected) else it }
    }

    fun removeAll() {
        tags = tags.map { it.copy(selected = false) }
    }

    fun selectedTags(): List<FilterTagUi> = tags.filter { it.selected }
}

@Composable
fun rememberTagsFilterSheetState(items: List<FilterTagUi>): TagsFilterSheetState {
    return remember(items) { TagsFilterSheetState(items) }
}
