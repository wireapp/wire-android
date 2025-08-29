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
        val d = LocalDensity.current
        val realW = with(d) { realImgWidth.coerceAtLeast(1).toDp() }
        val realH = with(d) { realImgHeight.coerceAtLeast(1).toDp() }

        val scaleToMax = minOf(maxW / realW, maxH / realH)
        val scaleToMin = max(minW / realW, minH / realH)

        val scale = when {
            realW in minW..maxW && realH in minH..maxH -> 1f
            scaleToMax < 1f -> scaleToMax
            scaleToMin > 1f -> if (allowUpscale) scaleToMin else 1f
            else -> 1f
        }

        val w = (realW * scale).coerceIn(if (allowUpscale) minW else dimensions().spacing0x, maxW)
        val h = (realH * scale).coerceIn(if (allowUpscale) minH else dimensions().spacing0x, maxH)

        return DpSize(w, h)
    }
}
