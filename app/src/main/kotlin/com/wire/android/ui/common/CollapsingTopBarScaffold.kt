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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.theme.wireDimensions
import kotlin.math.roundToInt

/**
 * @param maxBarElevation maximum elevation value available
 * @param topBarHeader topmost part of the top bar, usually the TopAppBar, [topBarCollapsing] element slides under it,
 * the lambda receives elevation value for the [topBarHeader]
 * @param topBarCollapsing collapsing part of the top bar
 * @param topBarFooter bar under the [topBarCollapsing], moves with it and ends up directly under [topBarHeader] when collapsed
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 * [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param content content of the screen
 * @param bottomBar bottom bar of the screen, typically a [NavigationBar]
 * @param floatingActionButton Main action button of the screen, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param isSwipeable if true then collapsing is enabled
 * @param snapOnFling on collapsing fling, only close the collapsible and don't carry the velocity to the scrollable
 * @param keepElevationWhenCollapsed if true then keep showing elevation also when scrolling children after top bar is already collapsed;
 * if false then hide elevation when approaching the end of the collapsing and don't show it when scrolling children
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CollapsingTopBarScaffold(
    maxBarElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    topBarHeader: @Composable (elevation: Dp) -> Unit,
    topBarCollapsing: @Composable () -> Unit,
    topBarFooter: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isSwipeable: Boolean = true,
    snapOnFling: Boolean = true,
    keepElevationWhenCollapsed: Boolean = false
) {
    val maxBarElevationPx = with(LocalDensity.current) { maxBarElevation.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = State.EXPANDED)
    var nestedOffsetState by rememberSaveable { mutableStateOf(0f) }
    var collapsingHeight by rememberSaveable { mutableStateOf(0) }
    val topBarElevationState by remember {
        derivedStateOf {
            if (keepElevationWhenCollapsed) {
                val value = -(swipeableState.offset.value + nestedOffsetState)
                listOf(value, maxBarElevationPx).minOrNull() ?: 0f
            } else {
                // hide elevation when approaching the end of the collapsing and don't show it when scrolling children
                val value = -swipeableState.offset.value
                listOf(value, collapsingHeight - value, maxBarElevationPx).minOrNull() ?: 0f
            }
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0) swipeableState.performDrag(available.y).toOffset()
            else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            swipeableState.performDrag(available.y).toOffset().also { nestedOffsetState += consumed.y }

        override suspend fun onPreFling(available: Velocity): Velocity =
            if (available.y < 0 && swipeableState.currentValue != State.COLLAPSED) {
                swipeableState.performFling(available.y)
                if (snapOnFling) available
                else Velocity.Zero
            } else Velocity.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            swipeableState.performFling(velocity = available.y)
            return super.onPostFling(consumed, available)
        }

        private fun Float.toOffset() = Offset(0f, this)
    }

    WireScaffold(
        topBar = { topBarHeader(with(LocalDensity.current) { topBarElevationState.toDp() }) },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        content = { internalPadding ->
            Layout(
                modifier = if (isSwipeable) {
                    Modifier
                        .padding(internalPadding)
                        .swipeable(
                            state = swipeableState,
                            orientation = Orientation.Vertical,
                            anchors = mapOf(0f to State.EXPANDED).let {
                                if (collapsingHeight > 0) it.plus(-collapsingHeight.toFloat() to State.COLLAPSED)
                                else it
                            }
                        )
                        .nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                        .padding(internalPadding)
                },
                content = {
                    Box(modifier = Modifier.layoutId("topBarCollapsing")) { topBarCollapsing() }
                    Box(modifier = Modifier.layoutId("topBarFooter")) { topBarFooter() }
                    Box(modifier = Modifier.layoutId("content")) { content() }
                },
                measurePolicy = { measurables, constraints ->
                    val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                    val collapsingPlaceable = measurables.first { it.layoutId == "topBarCollapsing" }.measure(measureConstraints)
                    val footerPlaceable = measurables.first { it.layoutId == "topBarFooter" }.measure(measureConstraints)
                    val contentPlaceable = measurables.first { it.layoutId == "content" }
                        .measure(measureConstraints.copy(maxHeight = constraints.maxHeight - footerPlaceable.height))
                    collapsingHeight = collapsingPlaceable.height
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val swipeOffset = swipeableState.offset.value.roundToInt()
                        contentPlaceable.placeRelative(0, collapsingPlaceable.height + footerPlaceable.height + swipeOffset)
                        footerPlaceable.placeRelative(0, collapsingPlaceable.height + swipeOffset)
                        collapsingPlaceable.placeRelative(0, swipeOffset)
                    }
                }
            )
        }
    )
}

private enum class State {
    EXPANDED,
    COLLAPSED;
}
