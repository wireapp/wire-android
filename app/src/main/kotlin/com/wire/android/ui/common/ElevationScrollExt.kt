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
    if (firstVisibleItemIndex == 0) minOf(firstVisibleItemScrollOffset.toFloat().dp, maxElevation)
    else maxElevation

fun ScrollState.topBarElevation(maxElevation: Dp): Dp = minOf(value.dp, maxElevation)

fun LazyListState.bottomBarElevation(maxElevation: Dp): Dp = layoutInfo.visibleItemsInfo.lastOrNull()?.let {
    if (it.index == layoutInfo.totalItemsCount - 1) minOf((it.offset + it.size - layoutInfo.viewportEndOffset).dp, maxElevation)
    else maxElevation
} ?: maxElevation

fun ScrollState.bottomBarElevation(maxElevation: Dp): Dp = minOf((maxValue - value).dp, maxElevation)

@Composable
fun LazyListState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun LazyListState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { bottomBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { bottomBarElevation(maxElevation) } }
