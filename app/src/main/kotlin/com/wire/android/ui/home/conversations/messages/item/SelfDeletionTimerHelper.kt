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
package com.wire.android.ui.home.conversations.messages.item

import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper

data class DeletionIconMetrics(
    val displayFractionLeft: Float,
    val elapsedFraction: Float,
    val emptySweepDegrees: Float,
    val backgroundAlpha: Float
)

enum class QuantizeStrategy { FLOOR, CEIL, NEAREST }

fun computeDeletionIconMetrics(
    fractionLeft: Float,
    backgroundAlpha: Float,
    discreteSteps: Int? = 8,
    strategy: QuantizeStrategy = QuantizeStrategy.FLOOR
): DeletionIconMetrics {
    val clamped = fractionLeft.coerceIn(0f, 1f)
    val display = if (discreteSteps == null || discreteSteps <= 0) {
        clamped
    } else {
        val s = discreteSteps.toFloat()
        val stepped = when (strategy) {
            QuantizeStrategy.FLOOR -> kotlin.math.floor(clamped * s)
            QuantizeStrategy.CEIL -> kotlin.math.ceil(clamped * s)
            QuantizeStrategy.NEAREST -> kotlin.math.round(clamped * s)
        } / s
        stepped.coerceIn(0f, 1f)
    }
    val elapsed = 1f - display
    val sweep = FULL_CIRCLE_DEGREES * elapsed
    return DeletionIconMetrics(
        displayFractionLeft = display,
        elapsedFraction = elapsed,
        emptySweepDegrees = sweep,
        backgroundAlpha = backgroundAlpha.coerceIn(0f, 1f)
    )
}

fun SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable.iconMetrics(
    discreteSteps: Int? = 8,
    strategy: QuantizeStrategy = QuantizeStrategy.FLOOR
): DeletionIconMetrics =
    computeDeletionIconMetrics(fractionLeft, alphaBackgroundColor(), discreteSteps, strategy)

private const val FULL_CIRCLE_DEGREES = 360f
internal const val START_ANGLE_TOP_DEG = -90f
internal const val STROKE_WIDTH_FRACTION = 0.08f
