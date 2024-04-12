/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

object WireBottomSheetDefaults {

    val WireBottomSheetShape: Shape @Composable get() = RoundedCornerShape(dimensions().corner16x, dimensions().corner16x, 0.dp, 0.dp)
    val WireSheetContainerColor: Color @Composable get() = colorsScheme().surface
    val WireSheetContentColor: Color @Composable get() = colorsScheme().onSurface
    val WireContainerColor: Color @Composable get() = colorsScheme().background
    val WireContentColor: Color @Composable get() = colorsScheme().onBackground
    val WireSheetTonalElevation: Dp @Composable get() = 0.dp

    @Composable
    fun WireDragHandle() {
        Box(
            Modifier
                .padding(vertical = dimensions().spacing12x)
                .size(width = dimensions().modalBottomSheetDividerWidth, height = dimensions().spacing4x)
                .background(color = colorsScheme().secondaryText, shape = RoundedCornerShape(size = dimensions().spacing2x))

        )
    }
}
