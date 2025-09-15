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
package com.wire.android.ui.home.conversations.messages

import com.wire.android.ui.home.conversations.messages.item.QuantizeStrategy
import com.wire.android.ui.home.conversations.messages.item.computeDeletionIconMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class DeletionIconMetricsTest {

    @Test
    fun `quantizes fraction to steps`() {
        val m = computeDeletionIconMetrics(
            fractionLeft = 0.83f,
            backgroundAlpha = 0.4f,
            discreteSteps = 4,
            strategy = QuantizeStrategy.FLOOR
        )
        assertEquals(0.75f, m.displayFractionLeft, 1e-6f)
        assertEquals(0.25f, m.elapsedFraction, 1e-6f)
        assertEquals(90f, m.emptySweepDegrees, 1e-6f) // 360 * 0.25
        assertEquals(0.4f, m.backgroundAlpha, 1e-6f)
    }

    @Test
    fun `no quantization keeps raw fraction`() {
        val m = computeDeletionIconMetrics(
            fractionLeft = 0.42f,
            backgroundAlpha = 0.0f,
            discreteSteps = null
        )
        assertEquals(0.42f, m.displayFractionLeft, 1e-6f)
        assertEquals(0.58f, m.elapsedFraction, 1e-6f)
        assertEquals(208.8f, m.emptySweepDegrees, 1e-3f)
    }

    @Test
    fun `clamps inputs to valid ranges`() {
        val m1 = computeDeletionIconMetrics(-0.3f, -1f, discreteSteps = 4)
        assertEquals(0f, m1.displayFractionLeft, 1e-6f)
        assertEquals(1f, m1.elapsedFraction, 1e-6f)
        assertEquals(360f, m1.emptySweepDegrees, 1e-6f)
        assertEquals(0f, m1.backgroundAlpha, 1e-6f)

        val m2 = computeDeletionIconMetrics(1.7f, 2f, discreteSteps = 4)
        assertEquals(1f, m2.displayFractionLeft, 1e-6f)
        assertEquals(0f, m2.elapsedFraction, 1e-6f)
        assertEquals(0f, m2.emptySweepDegrees, 1e-6f)
        assertEquals(1f, m2.backgroundAlpha, 1e-6f)
    }

    @Test
    fun `invariants hold`() {
        val m = computeDeletionIconMetrics(0.42f, 0.3f, discreteSteps = 8)
        assertEquals(1f, m.displayFractionLeft + m.elapsedFraction, 1e-6f)
        assertEquals(360f * m.elapsedFraction, m.emptySweepDegrees, 1e-6f)
    }

    @Test
    fun `quantization strategies behave correctly`() {
        val f = 0.62f
        val steps = 4

        val floor = computeDeletionIconMetrics(f, 0f, steps, QuantizeStrategy.FLOOR)
        assertEquals(0.50f, floor.displayFractionLeft, 1e-6f)

        val ceil = computeDeletionIconMetrics(f, 0f, steps, QuantizeStrategy.CEIL)
        assertEquals(0.75f, ceil.displayFractionLeft, 1e-6f)

        val nearestLow = computeDeletionIconMetrics(0.63f, 0f, steps, QuantizeStrategy.NEAREST)
        assertEquals(0.75f, nearestLow.displayFractionLeft, 1e-6f)

        val nearestHigh = computeDeletionIconMetrics(0.62f, 0f, steps, QuantizeStrategy.NEAREST)
        assertEquals(0.50f, nearestHigh.displayFractionLeft, 1e-6f)
    }

    @Test
    fun `exact step boundaries stay exact regardless of strategy`() {
        val f = 0.75f
        val steps = 4
        for (s in QuantizeStrategy.entries) {
            val m = computeDeletionIconMetrics(f, 0f, steps, s)
            assertEquals(0.75f, m.displayFractionLeft, 1e-6f)
            assertEquals(0.25f, m.elapsedFraction, 1e-6f)
            assertEquals(90f, m.emptySweepDegrees, 1e-6f)
        }
    }

    @Test
    fun `edge discreteSteps behave as expected`() {
        val none = computeDeletionIconMetrics(0.33f, 0f, 0)
        assertEquals(0.33f, none.displayFractionLeft, 1e-6f)

        val s1a = computeDeletionIconMetrics(0.01f, 0f, 1, QuantizeStrategy.FLOOR)
        assertEquals(0f, s1a.displayFractionLeft, 1e-6f)
        val s1b = computeDeletionIconMetrics(0.99f, 0f, 1, QuantizeStrategy.CEIL)
        assertEquals(1f, s1b.displayFractionLeft, 1e-6f)

        val many = computeDeletionIconMetrics(0.3333f, 0f, 1000, QuantizeStrategy.NEAREST)
        assertEquals(0.3333f, many.displayFractionLeft, 1e-3f)
    }
}
