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
package com.wire.android.tests.core.models
import java.util.NoSuchElementException
import java.util.regex.Pattern

enum class AccentColor(
    val id: Int,
    val color: String,
    val hexColor: String
) {
    Undefined(0, "Undefined", "#ffffff"),
    // Old colors
    StrongBlue(1, "StrongBlue", "#2391d3"),
    StrongLimeGreen(2, "StrongLimeGreen", "#00c800"),
    BrightYellow(3, "BrightYellow", "#febf02"),
    VividRed(4, "VividRed", "#fb0807"),
    BrightOrange(5, "BrightOrange", "#ff8900"),
    SoftPink(6, "SoftPink", "#fe5ebd"),
    Violet(7, "Violet", "#9c00fe"),
    // Redesign 2022
    Blue(1, "Blue", "#0667c8"),
    Green(2, "Green", "#1d7833"),
    Red(4, "Red", "#c20013"),
    Amber(5, "Amber", "#a25915"),
    Petrol(6, "Petrol", "#01718e"),
    Purple(7, "Purple", "#8944ab");

    override fun toString(): String = color

    companion object {
        fun getById(colorId: Int): AccentColor {
            return values().firstOrNull { it.id == colorId }
                ?: throw NoSuchElementException("Accent color id '$colorId' is unknown")
        }

        fun getByName(colorName: String): AccentColor {
            return values().firstOrNull { it.color.equals(colorName, ignoreCase = true) }
                ?: throw NoSuchElementException("Accent color name '$colorName' is unknown")
        }

        fun getByHex(colorHex: String): AccentColor {
            return values().firstOrNull { it.hexColor.equals(colorHex, ignoreCase = true) }
                ?: throw NoSuchElementException("Accent color hex '$colorHex' is unknown")
        }

        fun getByRgba(colorRgba: String): AccentColor {
            val colorHex = rgbaToHexColor(colorRgba)
            return values().firstOrNull { it.hexColor.equals(colorHex, ignoreCase = true) }
                ?: throw NoSuchElementException(
                    "Accent color rgba '$colorRgba' (hex '$colorHex') is unknown"
                )
        }

        private fun rgbaToHexColor(colorRgba: String): String {
            val pattern = Pattern.compile("^rgba?\\((\\d+), (\\d+), (\\d+).*\\)$")
            val matcher = pattern.matcher(colorRgba)
            var red = 255
            var green = 255
            var blue = 255

            if (matcher.find()) {
                red = matcher.group(1).toInt()
                green = matcher.group(2).toInt()
                blue = matcher.group(3).toInt()
            }

            return "#${red.toHex()}${green.toHex()}${blue.toHex()}"
        }

        private fun Int.toHex(): String = toString(16).padStart(2, '0')
    }
}
