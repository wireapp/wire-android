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
package com.wire.android.audioplayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AudioPlayerSkipTest {

    @Test
    fun givenPositionInMiddle_whenSkippingBack_thenTargetIsTwelveSecondsEarlier() {
        assertEquals(18_000, skipBackTarget(currentPositionMs = 30_000))
    }

    @Test
    fun givenPositionInMiddle_whenSkippingForward_thenTargetIsTwelveSecondsLater() {
        assertEquals(42_000, skipForwardTarget(currentPositionMs = 30_000, durationMs = 120_000))
    }

    @Test
    fun givenPositionCloserToStartThanSkip_whenSkippingBack_thenTargetIsClampedToZero() {
        assertEquals(0, skipBackTarget(currentPositionMs = 5_000))
    }

    @Test
    fun givenPositionCloserToEndThanSkip_whenSkippingForward_thenTargetIsClampedToDuration() {
        assertEquals(120_000, skipForwardTarget(currentPositionMs = 115_000, durationMs = 120_000))
    }

    @Test
    fun givenUnknownDuration_whenSkippingForward_thenTargetIsZero() {
        assertEquals(0, skipForwardTarget(currentPositionMs = 0, durationMs = 0))
    }
}
