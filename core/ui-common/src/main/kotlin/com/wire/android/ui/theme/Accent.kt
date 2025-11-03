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

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.R

@Suppress("MagicNumber")
enum class Accent(val accentId: Int) {
    Amber(5),
    Blue(1),
    Green(2),
    Purple(7),
    Red(4),
    Petrol(6),

    // unknown should always be last
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

@StringRes
fun Accent.resourceId(): Int = when (this) {
    Accent.Amber -> R.string.accent_color_amber
    Accent.Blue -> R.string.accent_color_blue
    Accent.Green -> R.string.accent_color_green
    Accent.Purple -> R.string.accent_color_purple
    Accent.Red -> R.string.accent_color_red
    Accent.Petrol -> R.string.accent_color_petrol
    Accent.Unknown -> R.string.accent_color_none
}
