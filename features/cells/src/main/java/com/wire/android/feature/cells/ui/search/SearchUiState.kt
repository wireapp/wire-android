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
package com.wire.android.feature.cells.ui.search

import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTypeUi
import com.wire.android.feature.cells.ui.search.filter.data.TypeFilter

data class SearchUiState(
    val availableTags: List<FilterTagUi> = emptyList(),
    val availableOwners: List<FilterOwnerUi> = emptyList(),
    val availableTypes: List<FilterTypeUi> = TypeFilter.typeItems,

    val showFilterByType: Boolean = false,
    val showFilterByTags: Boolean = false,
    val showFilterByOwner: Boolean = false,

    val filesWithPublicLink: Boolean = false,

    val isSearchActive: Boolean = true,
) {
    val tagsCount: Int get() = availableTags.count { it.selected }
    val typeCount: Int get() = availableTypes.count { it.selected }
    val ownerCount: Int get() = availableOwners.count { it.selected }

    val hasAnyFilter: Boolean
        get() = tagsCount > 0 || typeCount > 0 || ownerCount > 0 || filesWithPublicLink
}
