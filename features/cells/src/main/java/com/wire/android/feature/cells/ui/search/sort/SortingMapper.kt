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

fun SortingCriteria.toKaliumCriteria(): com.wire.kalium.cells.data.SortingCriteria =
    when (by) {
        SortBy.Default ->
            com.wire.kalium.cells.data.SortingCriteria.FOLDERS_FIRST_THEN_ALPHABETICAL

        SortBy.Modified ->
            com.wire.kalium.cells.data.SortingCriteria.MODIFICATION_TIME

        // The API does not support case-insensitive sorting
        // so we will use case-sensitive as a fallback until implemented
        // see https://wearezeta.atlassian.net/browse/WPB-24598
        SortBy.Name ->
            com.wire.kalium.cells.data.SortingCriteria.NAME_CASE_SENSITIVE

        SortBy.Size ->
            com.wire.kalium.cells.data.SortingCriteria.SIZE
    }
