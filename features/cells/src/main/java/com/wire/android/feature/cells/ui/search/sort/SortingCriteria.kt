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

enum class SortBy(val label: Int, val visible: Boolean = true) {
    Default(R.string.sort_by, false),
    Modified(R.string.sort_by_modified),
    Name(R.string.sort_by_name),
    Size(R.string.sort_by_size),
}

/**
 * Which way the arrow of the sort row points, telling apart the two options a sorting offers: up
 * for the one listed first, down for its reverse. This is how the sorting reads, not how it is
 * queried — see [SortingCriteria.isDescending] for that.
 */
@Suppress("MagicNumber")
enum class SortArrow(val rotationAngle: Float) {
    Up(0f),
    Down(180f),
}

sealed interface SortingCriteria {
    val by: SortBy
    val label: Int
    val arrow: SortArrow

    val isDescending: Boolean

    val rotationAngle: Float get() = arrow.rotationAngle

    data object FoldersFirst : SortingCriteria {
        override val by: SortBy = SortBy.Default
        override val label: Int = R.string.sort_by
        override val arrow: SortArrow = SortArrow.Up
        override val isDescending: Boolean = false
    }

    sealed class ByDate(
        override val label: Int,
        override val arrow: SortArrow,
        override val isDescending: Boolean,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Modified

        // The newest date is the largest timestamp, so it is the descending query that puts the
        // newest first — the opposite of how the name and size sortings are queried.
        data object NewestFirst : ByDate(R.string.sort_modified_newest_first, SortArrow.Up, isDescending = true)
        data object OldestFirst : ByDate(R.string.sort_modified_oldest_first, SortArrow.Down, isDescending = false)
    }

    sealed class ByName(
        override val label: Int,
        override val arrow: SortArrow,
        override val isDescending: Boolean,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Name

        data object AtoZ : ByName(R.string.sort_name_a_to_z, SortArrow.Up, isDescending = false)
        data object ZtoA : ByName(R.string.sort_name_z_to_a, SortArrow.Down, isDescending = true)
    }

    sealed class BySize(
        override val label: Int,
        override val arrow: SortArrow,
        override val isDescending: Boolean,
    ) : SortingCriteria {
        override val by: SortBy = SortBy.Size

        data object SmallestFirst : BySize(R.string.sort_size_smallest_first, SortArrow.Up, isDescending = false)
        data object LargestFirst : BySize(R.string.sort_size_largest_first, SortArrow.Down, isDescending = true)
    }
}
