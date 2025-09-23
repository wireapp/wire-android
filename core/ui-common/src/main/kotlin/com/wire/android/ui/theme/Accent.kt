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

import androidx.compose.ui.graphics.Color

@Suppress("MagicNumber")
enum class Accent(val accentId: Int) {
    Amber(5),
    Blue(1),
    Green(2),
    Purple(7),
    Red(4),
    Petrol(6),
    Unknown(0);

    companion object {
        fun fromAccentId(accentId: Int?): Accent = accentId?.let { entries.firstOrNull { it.accentId == accentId } } ?: Unknown
    }
}

class WireAccentColors(private val association: (Accent) -> Color) {
    fun getOrDefault(accent: Accent, default: Color): Color = when (accent) {
        Accent.Unknown -> default
        else -> association(accent)
    }
}
