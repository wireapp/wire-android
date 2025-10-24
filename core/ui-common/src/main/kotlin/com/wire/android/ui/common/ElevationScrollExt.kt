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

package com.wire.android.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions

fun LazyListState.topBarElevation(maxElevation: Dp): Dp =
    if (firstVisibleItemIndex == 0) {
        minOf(firstVisibleItemScrollOffset.toFloat().dp, maxElevation)
    } else {
        maxElevation
    }

fun ScrollState.topBarElevation(maxElevation: Dp): Dp = minOf(value.dp, maxElevation)

fun LazyListState.bottomBarElevation(maxElevation: Dp): Dp = layoutInfo.visibleItemsInfo.lastOrNull()?.let {
    if (it.index == layoutInfo.totalItemsCount - 1) {
        minOf((it.offset + it.size - layoutInfo.viewportEndOffset).dp, maxElevation)
    } else {
        maxElevation
    }
} ?: maxElevation

fun ScrollState.bottomBarElevation(maxElevation: Dp): Dp = minOf((maxValue - value).dp, maxElevation)

@Composable
fun LazyListState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember(this) { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember(this) { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun LazyListState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember(this) { derivedStateOf { bottomBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember(this) { derivedStateOf { bottomBarElevation(maxElevation) } }
