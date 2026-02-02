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
package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes

@Suppress("ArgumentListWrapping")
@Composable
fun CustomGroupAvatarDrawing(
    rightSideShapeColor: Color,
    middleSideShapeColor: Color,
    leftSideShapeColor: Color,
    modifier: Modifier = Modifier
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val originalWidth = 22f
            val originalHeight = 12f

            // Calculate the scale factors for both axes based on canvas size
            val scaleX = size.width / originalWidth
            val scaleY = size.height / originalHeight

            // Use the smaller scale factor to maintain aspect ratio
            val scale = minOf(scaleX, scaleY)

            // Calculate the offset to center the vector
            val offsetX = (size.width - originalWidth * scale) / 2
            val offsetY = (size.height - originalHeight * scale) / 2

            // Define left side of the canvas
            val leftSidePath = Path().apply {
                // First sub-path (circle-like shape)
                moveTo(4.283f * scale + offsetX, 5.982f * scale + offsetY)
                cubicTo(
                    3.212f * scale + offsetX,
                    5.982f * scale + offsetY,
                    2.314f * scale + offsetX,
                    5.028f * scale + offsetY,
                    2.314f * scale + offsetX,
                    3.823f * scale + offsetY
                )
                cubicTo(
                    2.308f * scale + offsetX,
                    2.64f * scale + offsetY,
                    3.218f * scale + offsetX,
                    1.702f * scale + offsetY,
                    4.283f * scale + offsetX,
                    1.702f * scale + offsetY
                )
                cubicTo(
                    5.349f * scale + offsetX,
                    1.702f * scale + offsetY,
                    6.259f * scale + offsetX,
                    2.623f * scale + offsetY,
                    6.259f * scale + offsetX,
                    3.817f * scale + offsetY
                )
                cubicTo(
                    6.259f * scale + offsetX,
                    5.028f * scale + offsetY,
                    5.355f * scale + offsetX,
                    5.982f * scale + offsetY,
                    4.283f * scale + offsetX,
                    5.982f * scale + offsetY
                )
                close()

                // Second sub-path (horizontal shape)
                moveTo(1.231f * scale + offsetX, 11.066f * scale + offsetY)
                cubicTo(
                    0.45f * scale + offsetX,
                    11.066f * scale + offsetY,
                    0.165f * scale + offsetX,
                    10.731f * scale + offsetY,
                    0.165f * scale + offsetX,
                    10.145f * scale + offsetY
                )
                cubicTo(
                    0.165f * scale + offsetX,
                    8.655f * scale + offsetY,
                    1.806f * scale + offsetX,
                    6.831f * scale + offsetY,
                    4.283f * scale + offsetX,
                    6.831f * scale + offsetY
                )
                cubicTo(
                    5.266f * scale + offsetX,
                    6.831f * scale + offsetY,
                    6.069f * scale + offsetX,
                    7.115f * scale + offsetY,
                    6.677f * scale + offsetX,
                    7.5f * scale + offsetY
                )
                cubicTo(
                    5.383f * scale + offsetX,
                    8.527f * scale + offsetY,
                    4.769f * scale + offsetX,
                    10.207f * scale + offsetY,
                    5.372f * scale + offsetX,
                    11.066f * scale + offsetY
                )
                lineTo(1.231f * scale + offsetX, 11.066f * scale + offsetY) // To close the path
            }

            drawPath(
                path = leftSidePath,
                color = leftSideShapeColor
            )

            // Define middle side of the canvas
            val middleSidePath = Path().apply {
                // First sub-path (circle-like shape)
                moveTo(10.9118f * scale + offsetX, 5.85596f * scale + offsetY)
                cubicTo(
                    9.67857f * scale + offsetX,
                    5.85596f * scale + offsetY,
                    8.65179f * scale + offsetX,
                    4.76221f * scale + offsetY,
                    8.64621f * scale + offsetX,
                    3.3727f * scale + offsetY
                )
                cubicTo(
                    8.64621f * scale + offsetX,
                    2.01667f * scale + offsetY,
                    9.68415f * scale + offsetX,
                    0.934082f * scale + offsetY,
                    10.9118f * scale + offsetX,
                    0.934082f * scale + offsetY
                )
                cubicTo(
                    12.1395f * scale + offsetX,
                    0.934082f * scale + offsetY,
                    13.1775f * scale + offsetX,
                    1.99993f * scale + offsetY,
                    13.1775f * scale + offsetX,
                    3.36154f * scale + offsetY
                )
                cubicTo(
                    13.1775f * scale + offsetX,
                    4.76221f * scale + offsetY,
                    12.1451f * scale + offsetX,
                    5.85596f * scale + offsetY,
                    10.9118f * scale + offsetX,
                    5.85596f * scale + offsetY
                )
                close()

                // Second sub-path (horizontal shape)
                moveTo(7.45201f * scale + offsetX, 11.0624f * scale + offsetY)
                cubicTo(
                    6.50335f * scale + offsetX,
                    11.0624f * scale + offsetY,
                    6.17969f * scale + offsetX,
                    10.7667f * scale + offsetY,
                    6.17969f * scale + offsetX,
                    10.2365f * scale + offsetY
                )
                cubicTo(
                    6.17969f * scale + offsetX,
                    8.80239f * scale + offsetY,
                    8.01562f * scale + offsetX,
                    6.8381f * scale + offsetY,
                    10.9118f * scale + offsetX,
                    6.8381f * scale + offsetY
                )
                cubicTo(
                    13.8025f * scale + offsetX,
                    6.8381f * scale + offsetY,
                    15.6384f * scale + offsetX,
                    8.80239f * scale + offsetY,
                    15.6384f * scale + offsetX,
                    10.2365f * scale + offsetY
                )
                cubicTo(
                    15.6384f * scale + offsetX,
                    10.7667f * scale + offsetY,
                    15.3147f * scale + offsetX,
                    11.0624f * scale + offsetY,
                    14.3661f * scale + offsetX,
                    11.0624f * scale + offsetY
                )
                lineTo(7.45201f * scale + offsetX, 11.0624f * scale + offsetY) // Close the path for the second sub-path
            }

            drawPath(
                path = middleSidePath,
                color = middleSideShapeColor
            )

            // Define right side of the canvas
            val rightSidePath = Path().apply {
                // First sub-path (circle-like shape)
                moveTo(17.716f * scale + offsetX, 5.982f * scale + offsetY)
                cubicTo(
                    16.639f * scale + offsetX,
                    5.982f * scale + offsetY,
                    15.741f * scale + offsetX,
                    5.028f * scale + offsetY,
                    15.741f * scale + offsetX,
                    3.817f * scale + offsetY
                )
                cubicTo(
                    15.741f * scale + offsetX,
                    2.623f * scale + offsetY,
                    16.651f * scale + offsetX,
                    1.702f * scale + offsetY,
                    17.716f * scale + offsetX,
                    1.702f * scale + offsetY
                )
                cubicTo(
                    18.782f * scale + offsetX,
                    1.702f * scale + offsetY,
                    19.692f * scale + offsetX,
                    2.64f * scale + offsetY,
                    19.686f * scale + offsetX,
                    3.823f * scale + offsetY
                )
                cubicTo(
                    19.686f * scale + offsetX,
                    5.028f * scale + offsetY,
                    18.788f * scale + offsetX,
                    5.982f * scale + offsetY,
                    17.716f * scale + offsetX,
                    5.982f * scale + offsetY
                )
                close()

                // Second sub-path (horizontal shape)
                moveTo(20.769f * scale + offsetX, 11.066f * scale + offsetY)
                lineTo(16.628f * scale + offsetX, 11.066f * scale + offsetY)
                cubicTo(
                    17.231f * scale + offsetX,
                    10.207f * scale + offsetY,
                    16.617f * scale + offsetX,
                    8.527f * scale + offsetY,
                    15.323f * scale + offsetX,
                    7.5f * scale + offsetY
                )
                cubicTo(
                    15.931f * scale + offsetX,
                    7.115f * scale + offsetY,
                    16.734f * scale + offsetX,
                    6.831f * scale + offsetY,
                    17.716f * scale + offsetX,
                    6.831f * scale + offsetY
                )
                cubicTo(
                    20.194f * scale + offsetX,
                    6.831f * scale + offsetY,
                    21.835f * scale + offsetX,
                    8.655f * scale + offsetY,
                    21.835f * scale + offsetX,
                    10.145f * scale + offsetY
                )
                cubicTo(
                    21.835f * scale + offsetX,
                    10.731f * scale + offsetY,
                    21.55f * scale + offsetX,
                    11.066f * scale + offsetY,
                    20.769f * scale + offsetX,
                    11.066f * scale + offsetY
                )
                close()
            }

            drawPath(
                path = rightSidePath,
                color = rightSideShapeColor
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCustomGroupAvatarDrawing() = WireTheme {
    CustomGroupAvatarDrawing(
        modifier = Modifier.size(300.dp),
        rightSideShapeColor = Color(0xFFC20013),
        middleSideShapeColor = Color(0xFF1D7833),
        leftSideShapeColor = Color(0xFF8944AB)
    )
}
