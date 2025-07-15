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
package util

sealed class NumberSequence {
    /**
     * Represents the data as a fixed-size array of integers.
     * @param array The actual IntArray.
     */
    data class Array(val array: IntArray) : NumberSequence() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Array

            if (!array.contentEquals(other.array)) return false

            return true
        }

        override fun hashCode(): Int {
            return array.contentHashCode()
        }
    }

    /**
     * Represents the data as a range of integers.
     * @param range The actual IntRange.
     */
    data class Range(val range: IntRange) : NumberSequence()
}
