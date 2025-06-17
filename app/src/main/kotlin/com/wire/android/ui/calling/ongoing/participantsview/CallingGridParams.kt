/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.calling.ongoing.participantsview

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.Dp

@Suppress("MagicNumber")
enum class CallingGridParams(
    val maxItemsPerPage: Int, // maximum number of participants to show on a single page
    val orientation: Orientation, // orientation of the list/grid
    val calculateColumnsAndRows: (itemsCount: Int) -> Pair<Int, Int>,
) {
    Portrait(
        maxItemsPerPage = 8,
        orientation = Orientation.Vertical,
        { itemsCount ->
            when (itemsCount) {
                in 0..3 -> 1 to itemsCount // up to 3 items show single column
                else -> 2 to ceilDiv(itemsCount, 2) // 4 or more items show grid with 2 columns
            }
        }
    ),
    Landscape(
        maxItemsPerPage = 8,
        orientation = Orientation.Horizontal,
        { itemsCount ->
            when (itemsCount) {
                in 0..4 -> itemsCount to 1 // up to 4 items show single row
                else -> ceilDiv(itemsCount, 2) to 2 // 4 or more items show grid with 2 rows
            }
        }
    ),
    Square(
        maxItemsPerPage = 9,
        orientation = Orientation.Vertical,
        { itemsCount ->
            when (itemsCount) {
                in 0..2 -> 1 to itemsCount // up to 2 items show single column
                in 3..6 -> 2 to ceilDiv(itemsCount, 2) // 4 to 6 items show grid with 2 columns
                else -> 3 to ceilDiv(itemsCount, 3) // 9 or more items show grid with 3 columns
            }
        }
    );

    companion object {
        fun fromScreenDimensions(width: Dp, height: Dp) = (width.value / height.value).let { ratio ->
            when {
                ratio < 3f / 4f -> Portrait
                ratio > 4f / 3f -> Landscape
                else -> Square
            }
        }
    }
}

private fun ceilDiv(dividend: Int, divisor: Int) = dividend.div(divisor) + if (dividend.rem(divisor) > 0) 1 else 0
