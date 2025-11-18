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
package com.wire.android.media.audiomessage

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioWavesMaskHelperTest {
    @Test
    fun givenWavesMask_whenMappedToNormalizedLoudness_thenTheResultIsCorrect() = runTest {
        val wavesMask = listOf(0, 5, 50, 100, 150, 200, 250, 255)
        val expectedNormalizedLoudness = byteArrayOf(0, 5, 50, 100, -106, -56, -6, -1) // UByteArray: [0, 5, 50, 100, 150, 200, 250, 255]
        val normalizedLoudness = wavesMask.toNormalizedLoudness()
        assert(expectedNormalizedLoudness.contentEquals(normalizedLoudness))
    }

    @Test
    fun givenNormalizedLoudness_whenMappedToWavesMask_thenTheResultIsCorrect() = runTest {
        val normalizedLoudness = byteArrayOf(0, 5, 50, 100, -106, -56, -6, -1) // UByteArray: [0, 5, 50, 100, 150, 200, 250, 255]
        val expectedWavesMask = listOf(0, 5, 50, 100, 150, 200, 250, 255)
        val wavesMask = normalizedLoudness.toWavesMask()
        assertEquals(expectedWavesMask, wavesMask)
    }

    @Test
    fun givenWavesMask_whenMappedToNormalizedLoudnessAndBack_thenTheResultIsEqualToOriginal() = runTest {
        val originalWavesMask = listOf(0, 5, 50, 100, 150, 200, 250, 255)
        val resultWavesMask = originalWavesMask.toNormalizedLoudness().toWavesMask()
        assertEquals(originalWavesMask, resultWavesMask)
    }

    @Test
    fun givenWavesMask_whenEqualizedStartingFrom0_thenTheResultIsCorrect() {
        val wavesMask = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        val expectedWavesMask = listOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40)
        val equalizedWavesMask = wavesMask.equalizedWavesMask(newMaxValue = 40, currentMaxValue = wavesMask.max(), startFrom1 = false)
        assertEquals(expectedWavesMask, equalizedWavesMask)
    }

    @Test
    fun givenWavesMask_whenEqualizedStartingFrom1_thenTheResultIsCorrect() {
        val wavesMask = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        val expectedWavesMask = listOf(1, 5, 9, 13, 17, 21, 24, 28, 32, 36, 40)
        val equalizedWavesMask = wavesMask.equalizedWavesMask(newMaxValue = 40, currentMaxValue = wavesMask.max(), startFrom1 = true)
        assertEquals(expectedWavesMask, equalizedWavesMask)
    }

    @Test
    fun givenWavesMask_whenSampled_thenTheResultIsCorrect() {
        val wavesMask = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        val expectedWavesMask = listOf(0, 25, 50, 75, 100)
        val sampledWavesMask = wavesMask.sampledWavesMask(amount = 5)
        assertEquals(expectedWavesMask, sampledWavesMask)
    }
}
