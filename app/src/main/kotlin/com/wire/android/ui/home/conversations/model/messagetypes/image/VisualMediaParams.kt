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

package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.wire.android.ui.common.dimensions
import kotlinx.serialization.Serializable
import kotlin.let

/**
 * Parameters describing visual media (image or video) used to calculate
 * normalized display size based on original resolution and screen configuration.
 */
@Serializable
data class VisualMediaParams(
    private val realMediaWidth: Int,
    private val realMediaHeight: Int
) {

    /**
     * Returns normalized dimensions preserving the original aspect ratio.
     * Size is limited by [maxBounds] and coerced to at least [minW] × [minH].
     */
    @Composable
    fun normalizedSize(
        minW: Dp = dimensions().messageImageMinWidth,
        minH: Dp = dimensions().messageImageMinHeight,
        maxBounds: MaxBounds = MaxBounds.DpBounds(
            dimensions().messageImageMaxWidth,
            dimensions().messageImageMaxHeight
        )
    ): NormalizedSize {
        if (realMediaWidth <= 0 || realMediaHeight <= 0) {
            return NormalizedSize(minW, minH, isPortrait = false)
        }

        val (effMaxW, effMaxH) = when (maxBounds) {
            is MaxBounds.DpBounds -> maxBounds.maxW to maxBounds.maxH
            is MaxBounds.ScreenFraction -> {
                fun Float.clampedFraction(): Float = coerceIn(0f, 1f)

                val conf = LocalConfiguration.current
                val screenW = conf.screenWidthDp.dp
                val screenH = conf.screenHeightDp.dp
                val w = maxBounds.maxWFraction.clampedFraction().let { screenW * it }
                val h = maxBounds.maxHFraction.clampedFraction().let { screenH * it }
                w to h
            }
        }

        val ratio = realMediaWidth.toFloat() / realMediaHeight.toFloat()
        val widthFromMaxH = effMaxH * ratio
        val heightFromMaxW = effMaxW / ratio

        val downW = min(effMaxW, widthFromMaxH)
        val downH = min(effMaxH, heightFromMaxW)

        val finalW = downW.coerceIn(minW, effMaxW)
        val finalH = downH.coerceIn(minH, effMaxH)

        val isPortrait = realMediaHeight > realMediaWidth
        return NormalizedSize(finalW, finalH, isPortrait)
    }
}

/**
 * Defines how maximum dimensions are applied during normalization.
 */
sealed interface MaxBounds {
    /** Uses fixed dp limits. */
    data class DpBounds(val maxW: Dp, val maxH: Dp) : MaxBounds

    /** Uses fractions (0f..1f) of the screen size. */
    data class ScreenFraction(val maxWFraction: Float, val maxHFraction: Float) : MaxBounds
}

/**
 * Normalized visual media dimensions with an orientation flag.
 */
data class NormalizedSize(
    val width: Dp,
    val height: Dp,
    val isPortrait: Boolean
)

fun NormalizedSize.size(): DpSize = DpSize(width, height)
