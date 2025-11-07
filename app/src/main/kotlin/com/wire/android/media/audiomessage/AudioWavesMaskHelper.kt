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
package com.wire.android.media.audiomessage

import kotlin.math.roundToInt

const val WAVE_MAX = 32

fun List<Int>.equalizedWavesMask(
    newMaxValue: Int = WAVE_MAX,
    currentMaxValue: Int = UByte.MAX_VALUE.toInt(), // normalized loudness can be up to 255 (UByte.MAX_VALUE)
    startFrom1: Boolean = true, // whether the minimum value should be 1 or 0
): List<Int> {
    if (this.isEmpty()) return listOf()
    val adjustedValue = if (startFrom1) 1 else 0
    val divider = currentMaxValue.toDouble() / (newMaxValue - adjustedValue)
    return if (divider == 0.0) {
        map { adjustedValue }
    } else {
        map { (it / divider).roundToInt() + adjustedValue }
    }
}

@Suppress("ReturnCount")
fun List<Int>.sampledWavesMask(amount: Int): List<Int> {
    if (this.isEmpty() || amount <= 0) return listOf()
    if (amount >= this.size) return this
    val res = MutableList(size = amount) { 1 }
    for (i in 0..amount - 2) {
        val index = i * (size - 1) / (amount - 1)
        val p = i * (size - 1) % (amount - 1)
        res[i] = ((p * this[index + 1]) + (((amount - 1) - p) * this[index])) / (amount - 1)
    }
    res[amount - 1] = this[size - 1] // done outside of loop to avoid out of bound access (0 * this[size])
    return res
}

fun ByteArray.toWavesMask(): List<Int> = this.map { it.toUByte().toInt() }.toList()
fun List<Int>.toNormalizedLoudness(): ByteArray = this.map { it.toUByte().toByte() }.toByteArray()
