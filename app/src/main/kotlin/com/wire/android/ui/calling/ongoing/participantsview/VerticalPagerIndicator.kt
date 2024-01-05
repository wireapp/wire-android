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

package com.wire.android.ui.calling.ongoing.participantsview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

/**
 * Updating VerticalPagerIndicator from com.google.accompanist.pager to have the ability to add a border to the pager indicator
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = colorsScheme().primary,
    inactiveColor: Color = colorsScheme().surface,
    inactiveBorderColor: Color = colorsScheme().uncheckedColor,
    inactiveBorderWidth: Dp = 1.dp,
    indicatorHeight: Dp = 8.dp,
    indicatorWidth: Dp = indicatorHeight,
    spacing: Dp = indicatorHeight,
    indicatorShape: Shape = CircleShape,
) {

    val indicatorHeightPx = LocalDensity.current.run { indicatorHeight.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val indicatorModifier = Modifier
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(color = inactiveColor, shape = indicatorShape)
                .border(inactiveBorderWidth, inactiveBorderColor, shape = indicatorShape)

            repeat(pagerState.pageCount) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(
                            0f,
                            (pagerState.pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )
                    IntOffset(
                        x = 0,
                        y = ((spacingPx + indicatorHeightPx) * scrollPosition).toInt(),
                    )
                }
                .size(width = indicatorWidth, height = indicatorHeight)
                .background(
                    color = activeColor,
                    shape = indicatorShape,
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun PreviewVerticalPagerIndicator() {
    VerticalPagerIndicator(
        modifier = Modifier.padding(dimensions().spacing4x),
        pagerState = rememberPagerState(pageCount = { 3 })
    )
}
