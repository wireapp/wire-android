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

package com.wire.android.ui.common

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @param topBarHeader topmost part of the top bar, usually the TopAppBar, [topBarCollapsing] element slides under it,
 * the lambda receives elevation value for the [topBarHeader]
 * @param topBarCollapsing collapsing part of the top bar
 * @param modifier modifier for the scaffold
 * @param maxBarElevation maximum elevation value available
 * @param topBarBackgroundColor background color of the top bar
 * @param topBarFooter bar under the [topBarCollapsing], moves with it and ends up directly under [topBarHeader] when collapsed
 * @param bottomBar bottom bar of the screen
 * @param floatingActionButton Main action button of the screen, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param collapsingEnabled if true then collapsing is enabled
 * @param snapOnFling on collapsing fling, only close the collapsible and don't carry the velocity to the scrollable
 * @param contentLazyListState state of the content lazy list, used for calculating elevations
 * @param content content of the screen
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollapsingTopBarScaffold(
    topBarHeader: @Composable () -> Unit,
    topBarCollapsing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    maxBarElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    topBarBackgroundColor: Color = MaterialTheme.wireColorScheme.background,
    topBarFooter: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    collapsingEnabled: Boolean = true,
    snapOnFling: Boolean = true,
    contentLazyListState: LazyListState? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var hasFooterSegment by remember { mutableStateOf(false) }
    var hasCollapsingSegment by remember { mutableStateOf(false) }
    val maxBarElevationPx = with(LocalDensity.current) { maxBarElevation.toPx() }
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = State.EXPANDED,
            anchors = calculateAnchors(collapsingEnabled, 0),
            positionalThreshold = { totalDistance: Float -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = SpringSpec(),
            decayAnimationSpec = splineBasedDecay(density),
        )
    }
    val topBarElevationState by remember(contentLazyListState, maxBarElevationPx) {
        derivedStateOf {
            with(density) {
                val collapsingHeight = anchoredDraggableState.calculateCollapsingHeight()
                val offset = -anchoredDraggableState.offset
                val scaledOffset = if (collapsingHeight > 0f && collapsingHeight < maxBarElevationPx) {
                    // if collapsingHeight is less than maxBarElevationPx then the offset needs to be scaled
                    (offset / collapsingHeight) * maxBarElevationPx
                } else {
                    offset
                }

                // hide top bar elevation when approaching the end of the collapsing
                listOf(maxBarElevationPx, scaledOffset, collapsingHeight - scaledOffset).min().toDp()
            }
        }
    }
    val topBarContainerElevationState by remember(contentLazyListState, maxBarElevationPx) {
        derivedStateOf {
            with(density) {
                // start adding elevation to the whole container after fully collapsed
                contentLazyListState.calculateContentOffset(maxBarElevationPx).toDp()
            }
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0 && collapsingEnabled) {
                anchoredDraggableState.dispatchRawDelta(delta = available.y).toOffset()
            } else {
                Offset.Zero
            }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (collapsingEnabled) {
                anchoredDraggableState.dispatchRawDelta(delta = available.y).toOffset()
            } else {
                Offset.Zero
            }

        override suspend fun onPreFling(available: Velocity): Velocity =
            if (available.y < 0 && anchoredDraggableState.currentValue != State.COLLAPSED && collapsingEnabled) {
                anchoredDraggableState.settle(velocity = available.y)
                if (snapOnFling) available else Velocity.Zero
            } else {
                Velocity.Zero
            }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (collapsingEnabled) {
                anchoredDraggableState.settle(velocity = available.y)
            }
            return super.onPostFling(consumed, available)
        }

        private fun Float.toOffset() = Offset(0f, this)
    }

    LaunchedEffect(contentLazyListState) {
        anchoredDraggableState.animateTo(
            when {
                contentLazyListState == null -> State.EXPANDED
                contentLazyListState.firstVisibleItemIndex > 0 || contentLazyListState.firstVisibleItemScrollOffset > 0 -> State.COLLAPSED
                else -> State.EXPANDED
            }
        )
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            Surface(
                color = topBarBackgroundColor,
                shadowElevation = if (hasFooterSegment || hasCollapsingSegment) topBarElevationState else topBarContainerElevationState,
            ) {
                topBarHeader()
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        content = { internalPadding ->
            Layout(
                modifier = if (collapsingEnabled && anchoredDraggableState.anchors.size > 1) {
                    Modifier
                        .padding(internalPadding)
                        .anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Vertical,
                        )
                        .nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                        .padding(internalPadding)
                },
                content = {
                    Surface(
                        modifier = Modifier.fillMaxWidth().layoutId("topBarContainer"),
                        color = topBarBackgroundColor,
                        shadowElevation = topBarContainerElevationState
                    ) {}
                    Box(modifier = Modifier.fillMaxWidth().layoutId("topBarCollapsing")) {
                        topBarCollapsing()
                    }
                    Box(modifier = Modifier.fillMaxWidth().layoutId("topBarFooter")) {
                        topBarFooter()
                    }
                    Box(modifier = Modifier.fillMaxWidth().layoutId("content")) {
                        content()
                    }
                },
                measurePolicy = { measurables, constraints ->
                    val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                    val collapsingPlaceable = measurables.first { it.layoutId == "topBarCollapsing" }.measure(measureConstraints)
                    val footerPlaceable = measurables.first { it.layoutId == "topBarFooter" }.measure(measureConstraints)
                    val containerPlaceable = measurables.first { it.layoutId == "topBarContainer" }.measure(
                        measureConstraints.copy(
                            minHeight = collapsingPlaceable.height + footerPlaceable.height,
                            maxHeight = collapsingPlaceable.height + footerPlaceable.height
                        )
                    )
                    val contentPlaceable = measurables.first { it.layoutId == "content" }.measure(
                        measureConstraints.copy(
                            maxHeight = if (collapsingEnabled) {
                                constraints.maxHeight - footerPlaceable.height
                            } else {
                                constraints.maxHeight - collapsingPlaceable.height - footerPlaceable.height
                            }
                        )
                    )
                    hasCollapsingSegment = collapsingPlaceable.height > 0
                    hasFooterSegment = footerPlaceable.height > 0
                    anchoredDraggableState.updateAnchors(calculateAnchors(collapsingEnabled, collapsingPlaceable.height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val swipeOffset = anchoredDraggableState.offset.roundToInt()
                        contentPlaceable.placeRelative(0, collapsingPlaceable.height + footerPlaceable.height + swipeOffset)
                        containerPlaceable.placeRelative(0, swipeOffset)
                        footerPlaceable.placeRelative(0, collapsingPlaceable.height + swipeOffset)
                        collapsingPlaceable.placeRelative(0, swipeOffset)
                    }
                }
            )
        }
    )
}

private fun LazyListState?.calculateContentOffset(maxValue: Float) = when {
    this == null -> 0f
    firstVisibleItemIndex == 0 -> min(firstVisibleItemScrollOffset.toFloat(), maxValue)
    else -> maxValue
}

@OptIn(ExperimentalFoundationApi::class)
private fun AnchoredDraggableState<State>.calculateCollapsingHeight() = anchors.positionOf(State.COLLAPSED).let {
    if (it.isNaN()) 0f else -it
}

@OptIn(ExperimentalFoundationApi::class)
private fun calculateAnchors(isSwipeable: Boolean, collapsingHeight: Int) = DraggableAnchors {
    State.EXPANDED at 0f
    if (isSwipeable && collapsingHeight > 0) State.COLLAPSED at -collapsingHeight.toFloat()
}

private enum class State {
    EXPANDED,
    COLLAPSED;
}
