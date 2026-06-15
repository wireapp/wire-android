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
package com.wire.android.ui.common.bottomsheet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.lerp
import com.wire.android.ui.common.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun WireDragHandle(
    modifier: Modifier = defaultDragHandleModifier(),
    progress: Float = 0f, // 0f: line, 1f: arrow
    color: Color = colorsScheme().secondaryText
) {
    val animatedProgress by animateFloatAsState(targetValue = progress)
    val thickness = dimensions().spacing3x
    val lineHalfWidth = dimensions().spacing48x / 2
    val arrowWingWidth = dimensions().spacing28x / 2
    val arrowWingHeight = dimensions().spacing8x

    Canvas(
        modifier = modifier
            .padding(dimensions().spacing6x)
            .size(width = (lineHalfWidth * 2) + thickness, height = arrowWingHeight + thickness)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val capRadius = thickness.toPx() / 2
        val tipY = lerp(centerY, capRadius, animatedProgress)
        val endY = lerp(centerY, size.height - capRadius, animatedProgress)

        fun drawWing(multiplier: Float) {
            val endX = centerX + (lerp(lineHalfWidth.toPx(), arrowWingWidth.toPx(), animatedProgress) * multiplier)
            drawLine(
                color = color,
                start = Offset(centerX, tipY),
                end = Offset(endX, endY),
                strokeWidth = thickness.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawWing(-1f) // Left
        drawWing(1f) // Right
    }
}

@Composable
private fun defaultDragHandleModifier(): Modifier {
    val defaultContentDescription = stringResource(R.string.content_description_drag_handle)
    return Modifier.semantics {
        this.contentDescription = defaultContentDescription
    }
}

@PreviewMultipleThemes
@Composable
fun WireDragHandlePreview_Line() = WireTheme {
    WireDragHandle(progress = 0f)
}

@PreviewMultipleThemes
@Composable
fun WireDragHandlePreview_Arrow() = WireTheme {
    WireDragHandle(progress = 1f)
}
