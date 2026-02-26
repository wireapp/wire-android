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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi

@Stable
class OwnersFilterSheetState(
    initialItems: List<FilterOwnerUi>
) {
    private val initialById = initialItems.associateBy { it.id }

    var owners by mutableStateOf(initialItems)
        private set

    var query by mutableStateOf("")
        private set

    val hasChanges: Boolean
        get() = owners.any { o -> initialById[o.id]?.selected != o.selected }

    val filteredOwners: List<FilterOwnerUi>
        get() {
            val q = query.trim()
            return if (q.isBlank()) {
                owners
            } else {
                owners.filter {
                    it.displayName.contains(q, ignoreCase = true) ||
                            it.handle.contains(q, ignoreCase = true)
                }
            }
        }

    fun onQueryChange(text: String) {
        query = text
    }

    fun toggleOwner(id: String) {
        owners = owners.map { if (it.id == id) it.copy(selected = !it.selected) else it }
    }

    fun removeAll() {
        owners = owners.map { it.copy(selected = false) }
    }

    fun selectedOwners(): List<FilterOwnerUi> = owners.filter { it.selected }
}

@Composable
fun rememberOwnersFilterSheetState(items: List<FilterOwnerUi>): OwnersFilterSheetState {
    return remember(items) { OwnersFilterSheetState(items) }
}
