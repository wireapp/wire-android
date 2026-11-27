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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo

/**
 * What to search the files saved for offline use for. Only their name can be searched: none of the filters apply.
 *
 * @param currentPath remote path of the folder searched, null for All files.
 */
internal data class OfflineSearch(
    val currentPath: String?,
    val query: String,
    val sortingCriteria: SortingCriteria,
)

/**
 * The files saved for offline use matching [search], matched and sorted the way the server does it: a query searches
 * the folder and its sub folders, filtering and sorting only the folder itself.
 *
 * Done in memory rather than in SQL on purpose: the files saved for offline use are few, being stored on the device,
 * and are all read anyway, so a query wouldn't save any database access. It also keeps the folder rules of
 * [relativePathIn] shared with the offline listing, and the matching and sorting by name case insensitive beyond ASCII,
 * which SQLite's LIKE and LOWER are not. Worth moving to SQL if there can be thousands of them, or to page them.
 */
internal fun List<OfflineFileInfo>.matching(search: OfflineSearch): List<OfflineFileInfo> =
    filter { it.isIn(search.currentPath, recursive = search.query.isNotEmpty()) }
        .filter { search.query.isEmpty() || it.name.contains(search.query, ignoreCase = true) }
        .sortedWith(search.sortingCriteria.comparator())

// All files has no folders: every file is in it.
@Suppress("ReturnCount")
private fun OfflineFileInfo.isIn(folderPath: String?, recursive: Boolean): Boolean {
    if (folderPath == null) return true
    val relativePath = relativePathIn(folderPath) ?: return false
    return recursive || '/' !in relativePath
}

// Files only: there are no folders to put first, so the default sorting is by name, like the offline listing.
private fun SortingCriteria.comparator(): Comparator<OfflineFileInfo> {
    val ascending: Comparator<OfflineFileInfo> = when (by) {
        SortBy.Modified -> compareBy { it.modifiedAt ?: it.downloadedAt }
        SortBy.Size -> compareBy { it.size ?: 0L }
        SortBy.Name, SortBy.Default -> compareBy { it.name.lowercase() }
    }
    return if (isDescending) ascending.reversed() else ascending
}
