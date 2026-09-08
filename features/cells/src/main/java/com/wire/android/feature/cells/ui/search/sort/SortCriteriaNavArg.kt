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

import kotlinx.serialization.Serializable

/**
 * Navigation-safe enum representation of [SortingCriteria].
 * Passed as a nav arg when entering the search screen so the search view
 * inherits the sort state that was active in the parent view.
 */
@Serializable
enum class SortCriteriaNavArg {
    FoldersFirst,
    NewestFirst,
    OldestFirst,
    NameAZ,
    NameZA,
    SizeSmallest,
    SizeLargest,
}

fun SortingCriteria.toNavArg(): SortCriteriaNavArg = when (this) {
    SortingCriteria.FoldersFirst -> SortCriteriaNavArg.FoldersFirst
    SortingCriteria.ByDate.NewestFirst -> SortCriteriaNavArg.NewestFirst
    SortingCriteria.ByDate.OldestFirst -> SortCriteriaNavArg.OldestFirst
    SortingCriteria.ByName.AtoZ -> SortCriteriaNavArg.NameAZ
    SortingCriteria.ByName.ZtoA -> SortCriteriaNavArg.NameZA
    SortingCriteria.BySize.SmallestFirst -> SortCriteriaNavArg.SizeSmallest
    SortingCriteria.BySize.LargestFirst -> SortCriteriaNavArg.SizeLargest
}

fun SortCriteriaNavArg.toSortingCriteria(): SortingCriteria = when (this) {
    SortCriteriaNavArg.FoldersFirst -> SortingCriteria.FoldersFirst
    SortCriteriaNavArg.NewestFirst -> SortingCriteria.ByDate.NewestFirst
    SortCriteriaNavArg.OldestFirst -> SortingCriteria.ByDate.OldestFirst
    SortCriteriaNavArg.NameAZ -> SortingCriteria.ByName.AtoZ
    SortCriteriaNavArg.NameZA -> SortingCriteria.ByName.ZtoA
    SortCriteriaNavArg.SizeSmallest -> SortingCriteria.BySize.SmallestFirst
    SortCriteriaNavArg.SizeLargest -> SortingCriteria.BySize.LargestFirst
}
