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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.ui.common.dimensions
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class ImageMessageParams(
    private val realImgWidth: Int,
    private val realImgHeight: Int,
    val allowUpscale: Boolean = true
) {
    @Composable
    fun normalizedSize(
        minW: Dp = dimensions().messageImageMinWidth,
        minH: Dp = dimensions().messageImageMinHeight,
        maxW: Dp = dimensions().messageImageMaxWidth,
        maxH: Dp = dimensions().messageImageMaxHeight
    ): DpSize {
        if (realImgWidth <= 0 || realImgHeight <= 0) {
            return DpSize(minW, minH)
        }

        val density = LocalDensity.current
        val realW = with(density) { realImgWidth.toDp() }
        val realH = with(density) { realImgHeight.toDp() }

        val widthScale = maxW / realW
        val heightScale = maxH / realH
        val scaleToFit = min(widthScale, heightScale)

        val minWidthScale = minW / realW
        val minHeightScale = minH / realH
        val scaleToMin = max(minWidthScale, minHeightScale)

        val scale = when {
            realW in minW..maxW && realH in minH..maxH -> 1f
            scaleToFit < 1f -> scaleToFit
            scaleToMin > 1f -> if (allowUpscale) scaleToMin else 1f
            else -> 1f
        }

        val scaledW = realW * scale
        val scaledH = realH * scale

        return DpSize(scaledW, scaledH)
    }
}
