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

package com.wire.android.feature.cells.ui.search.sort

import com.wire.android.feature.cells.R

enum class SortBy(val label: Int) {
    Modified(R.string.sort_by_modified),
    Name(R.string.sort_by_name),
    Size(R.string.sort_by_size),
}

enum class SortDirection(val isDescending: Boolean) {
    Asc(false),
    Desc(true);

    val rotationAngle: Float get() = if (isDescending) 180f else 0f
}

sealed interface SortingCriteria {
    val by: SortBy
    val label: Int
    val direction: SortDirection

    val isDescending: Boolean get() = direction.isDescending
    val rotationAngle: Float get() = direction.rotationAngle

    sealed class Modified(
        override val label: Int,
        override val direction: SortDirection,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Modified

        data object NewestFirst : Modified(R.string.sort_modified_newest_first, SortDirection.Asc)
        data object OldestFirst : Modified(R.string.sort_modified_oldest_first, SortDirection.Desc)
    }

    sealed class Name(
        override val label: Int,
        override val direction: SortDirection,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Name

        data object AtoZ : Name(R.string.sort_name_a_to_z, SortDirection.Asc)
        data object ZtoA : Name(R.string.sort_name_z_to_a, SortDirection.Desc)
    }

    sealed class Size(
        override val label: Int,
        override val direction: SortDirection,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Size

        data object SmallestFirst : Size(R.string.sort_size_smallest_first, SortDirection.Asc)
        data object LargestFirst : Size(R.string.sort_size_largest_first, SortDirection.Desc)
    }
}
