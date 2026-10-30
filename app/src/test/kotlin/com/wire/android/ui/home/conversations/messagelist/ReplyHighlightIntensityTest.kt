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
package com.wire.android.ui.home.conversations.messagelist

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReplyHighlightIntensityTest {

    @Test
    fun givenAnimationsEnabled_whenPulsing_thenRisesToPeakAndSettlesToHold() {
        assertEquals(0f, intensity(0))
        val rising = intensity(REPLY_HIGHLIGHT_PULSE_MS / 4)
        assertTrue(rising > 0f && rising < 1f)
        assertEquals(1f, intensity(REPLY_HIGHLIGHT_PULSE_MS / 2))
        val settling = intensity(REPLY_HIGHLIGHT_PULSE_MS * 3 / 4)
        assertTrue(settling < 1f && settling > REPLY_HIGHLIGHT_HOLD_INTENSITY)
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(REPLY_HIGHLIGHT_PULSE_MS))
    }

    @Test
    fun givenAnimationsEnabled_whenHolding_thenStaysAtHoldUntilHoldEnds() {
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(1_500))
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(REPLY_HIGHLIGHT_HOLD_UNTIL_MS - 1))
    }

    @Test
    fun givenAnimationsEnabled_whenFading_thenGoesFromHoldToZero() {
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(REPLY_HIGHLIGHT_HOLD_UNTIL_MS))
        val fading = intensity(REPLY_HIGHLIGHT_HOLD_UNTIL_MS + REPLY_HIGHLIGHT_FADE_MS / 2)
        assertTrue(fading > 0f && fading < REPLY_HIGHLIGHT_HOLD_INTENSITY)
        assertEquals(0f, intensity(REPLY_HIGHLIGHT_TOTAL_MS))
        assertEquals(0f, intensity(REPLY_HIGHLIGHT_TOTAL_MS + 1_000))
    }

    @Test
    fun givenAnimationsDisabled_thenStaticTintForHoldDurationThenNothing() {
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(0, animationsEnabled = false))
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(REPLY_HIGHLIGHT_PULSE_MS / 2, animationsEnabled = false))
        assertEquals(REPLY_HIGHLIGHT_HOLD_INTENSITY, intensity(REPLY_HIGHLIGHT_HOLD_UNTIL_MS - 1, animationsEnabled = false))
        assertEquals(0f, intensity(REPLY_HIGHLIGHT_HOLD_UNTIL_MS, animationsEnabled = false))
    }

    @Test
    fun givenNegativeElapsedTime_thenNothing() {
        assertEquals(0f, intensity(-1))
        assertEquals(0f, intensity(-1, animationsEnabled = false))
    }

    private fun intensity(elapsedMillis: Long, animationsEnabled: Boolean = true) =
        replyHighlightIntensity(elapsedMillis, animationsEnabled)
}
