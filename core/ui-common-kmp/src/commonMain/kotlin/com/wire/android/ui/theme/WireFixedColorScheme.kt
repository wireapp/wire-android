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
package com.wire.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class WireFixedColorScheme(
    val sketchColorPalette: List<Color>,
)

val DefaultWireFixedColorScheme = WireFixedColorScheme(
    sketchColorPalette = listOf(
        Color.Black,
        Color.White,
        WireColorPalette.LightBlue500,
        WireColorPalette.LightGreen550,
        WireColorPalette.DarkAmber500,
        WireColorPalette.LightRed500,
        WireColorPalette.Orange,
        WireColorPalette.LightPurple600,
        WireColorPalette.Brown,
        WireColorPalette.Turquoise,
        WireColorPalette.DarkBlue500,
        WireColorPalette.DarkGreen500,
        WireColorPalette.DarkPetrol500,
        WireColorPalette.DarkPurple500,
        WireColorPalette.DarkRed500,
        WireColorPalette.Pink,
        WireColorPalette.Chocolate,
        WireColorPalette.Gray70,
    )
)
