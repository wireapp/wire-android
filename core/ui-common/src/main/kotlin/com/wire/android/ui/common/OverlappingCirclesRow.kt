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
package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.withAccent
import com.wire.android.util.PreviewMultipleThemes
import kotlin.math.max

@Composable
fun OverlappingCirclesRow(
    overlapSize: Dp,
    overlapCutoutSize: Dp,
    overlapDirection: OverlapDirection,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Layout(
        modifier = modifier,
        content = {
            var sizeAndPositionList by remember(items) {
                mutableStateOf(List(items.size) { SizeAndPosition(Size.Zero, Offset.Zero) })
            }
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .onGloballyPositioned {
                            sizeAndPositionList = sizeAndPositionList.toMutableList().also { list ->
                                list[index] = SizeAndPosition(size = it.size.toSize(), position = it.positionInParent())
                            }
                        }
                        .drawWithContent { // this cuts out previous items in the overlap area if overlapCutoutBorderSize is set
                            with(drawContext.canvas.nativeCanvas) {
                                val checkPoint = saveLayer(null, null)
                                drawContent() // draw the original content first so that we can cut it out with the overlap area
                                if (overlapCutoutSize > 0.dp) {
                                    // find all item indexes that overlap the current item based on the overlap direction
                                    val overlappingIndexes = when (overlapDirection) {
                                        OverlapDirection.StartOnTop -> 0 until index
                                        OverlapDirection.EndOnTop -> (index + 1) until sizeAndPositionList.size
                                    }
                                    overlappingIndexes.forEach { overlappingIndex ->
                                        val (overlapping, current) = sizeAndPositionList[overlappingIndex] to sizeAndPositionList[index]
                                        // cut out the overlap area from the current item by drawing a rounded rect with BlendMode.Clear
                                        drawRoundRect(
                                            color = Color.Black,
                                            cornerRadius = CornerRadius(
                                                x = overlapping.size.minDimension,
                                                y = overlapping.size.minDimension
                                            ),
                                            size = overlapping.size.withBorder(overlapCutoutSize.toPx()),
                                            topLeft = overlapping.position.withBorder(overlapCutoutSize.toPx()).minus(current.position),
                                            blendMode = BlendMode.Clear
                                        )
                                    }
                                }
                                restoreToCount(checkPoint)
                            }
                        },
                    content = {
                        item()
                    }
                )
            }
        },
        measurePolicy = { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val height = placeables.maxOf { it.height }
            val width = placeables.filter { it.width > 0 }.let { nonEmptyPlaceables ->
                nonEmptyPlaceables.mapIndexed { index, item ->
                    when (index) {
                        nonEmptyPlaceables.lastIndex -> max(item.width, overlapSize.roundToPx())
                        else -> (item.width - overlapSize.roundToPx()).coerceAtLeast(0)
                    }
                }
            }.sum()
            layout(width, height) {
                var x = 0
                for (i in placeables.indices) {
                    val y = (height - placeables[i].height) / 2
                    val zIndex = if (overlapDirection == OverlapDirection.EndOnTop) i else placeables.size - i
                    placeables[i].placeRelative(x, y, zIndex.toFloat())
                    x += (placeables[i].width - overlapSize.roundToPx()).coerceAtLeast(0)
                }
            }
        }
    )
}

enum class OverlapDirection { StartOnTop, EndOnTop }
private data class SizeAndPosition(val size: Size, val position: Offset)

private fun Size.withBorder(borderWidth: Float) = if (isEmpty()) this else Size(width + (2 * borderWidth), height + (2 * borderWidth))
private fun Offset.withBorder(borderWidth: Float) = Offset(x - borderWidth, y - borderWidth)

@Composable
private fun OverlappingCirclesRowPreview(
    overlapCutoutBorderSize: Dp,
    overlapDirection: OverlapDirection,
    layoutDirection: LayoutDirection,
    count: Int = 6
) = WireTheme {
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        OverlappingCirclesRow(
            overlapSize = dimensions().spacing10x,
            overlapCutoutSize = overlapCutoutBorderSize,
            overlapDirection = overlapDirection,
            items = List(count) { index ->
                @Composable {
                    val accent = Accent.entries[index % Accent.entries.size]
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(dimensions().spacing32x)
                            .background(color = colorsScheme().withAccent(accent).primary, shape = CircleShape)
                            .padding(dimensions().spacing4x),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_input_mandatory),
                            contentDescription = "",
                            tint = colorsScheme().withAccent(accent).onPrimary,
                            modifier = Modifier.size(dimensions().spacing16x)
                        )
                    }
                }
            }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithCutout_StartOnTop_LTR_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing2x, OverlapDirection.StartOnTop, LayoutDirection.Ltr)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithCutout_StartOnTop_RTL_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing2x, OverlapDirection.StartOnTop, LayoutDirection.Rtl)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithCutout_EndOnTop_LTR_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing2x, OverlapDirection.EndOnTop, LayoutDirection.Ltr)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithCutout_EndOnTop_RTL_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing2x, OverlapDirection.EndOnTop, LayoutDirection.Rtl)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithoutCutout_StartOnTop_LTR_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing0x, OverlapDirection.StartOnTop, LayoutDirection.Ltr)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithoutCutout_StartOnTop_RTL_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing0x, OverlapDirection.StartOnTop, LayoutDirection.Rtl)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithoutCutout_EndOnTop_LTR_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing0x, OverlapDirection.EndOnTop, LayoutDirection.Ltr)

@PreviewMultipleThemes
@Composable
fun OverlappingCirclesRow_WithoutCutout_EndOnTop_RTL_Preview() =
    OverlappingCirclesRowPreview(dimensions().spacing0x, OverlapDirection.EndOnTop, LayoutDirection.Rtl)
