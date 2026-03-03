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

import com.wire.android.feature.cells.ui.search.filter.FilterTypes

enum class DriveScreenType(val filters: List<FilterTypes>) {
    SHARED_DRIVE(
        listOf(
            FilterTypes.TAGS,
            FilterTypes.TYPE,
            FilterTypes.OWNER,
            FilterTypes.LINK
        )
    ),
    DRIVE(
        listOf(
            FilterTypes.TAGS,
            FilterTypes.TYPE,
            FilterTypes.OWNER,
            FilterTypes.CONVERSATION,
            FilterTypes.LINK
        )
    )
}
