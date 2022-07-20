package com.wire.android.ui.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.wire.android.ui.theme.wireDimensions
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CollapsingTopBarScaffold(
    maxBarElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation,
    topBarHeader: @Composable (elevation: Dp) -> Unit,
    topBarCollapsing: @Composable () -> Unit,
    topBarFooter: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
    contentFooter: @Composable () -> Unit = {}
) {
    val maxBarElevationPx = with(LocalDensity.current) { maxBarElevation.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = State.EXPANDED)
    var collapsingHeight by remember { mutableStateOf(0) }
    val topBarElevationState by remember {
        derivedStateOf {
            val value = -swipeableState.offset.value
            listOf(value, collapsingHeight - value, maxBarElevationPx).minOrNull() ?: 0f
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0) swipeableState.performDrag(available.y).toOffset()
            else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            swipeableState.performDrag(available.y).toOffset()

        override suspend fun onPreFling(available: Velocity): Velocity =
            if (available.y < 0 && swipeableState.currentValue != State.COLLAPSED) {
                swipeableState.performFling(available.y)
                available
            } else Velocity.Zero


        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            swipeableState.performFling(velocity = available.y)
            return super.onPostFling(consumed, available)
        }

        private fun Float.toOffset() = Offset(0f, this)
    }

    Scaffold(
        topBar = { topBarHeader(with(LocalDensity.current) { topBarElevationState.toDp() }) },
        snackbarHost = snackbarHost,
        content = { internalPadding ->
            Layout(
                modifier = Modifier
                    .padding(internalPadding)
                    .swipeable(
                        state = swipeableState,
                        orientation = Orientation.Vertical,
                        anchors = mapOf(0f to State.EXPANDED).let {
                            if (collapsingHeight > 0) it.plus(-collapsingHeight.toFloat() to State.COLLAPSED)
                            else it
                        }
                    )
                    .nestedScroll(nestedScrollConnection),
                content = {
                    Box(modifier = Modifier.layoutId("topBarCollapsing")) { topBarCollapsing() }
                    Box(modifier = Modifier.layoutId("topBarFooter")) { topBarFooter() }
                    Box(modifier = Modifier.layoutId("content")) { content() }
                    Box(modifier = Modifier.layoutId("contentFooter")) { contentFooter() }
                },
                measurePolicy = { measurables, constraints ->
                    val measureConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                    val collapsingPlaceable = measurables.first { it.layoutId == "topBarCollapsing" }.measure(measureConstraints)
                    val footerPlaceable = measurables.first { it.layoutId == "topBarFooter" }.measure(measureConstraints)
                    val contentFooterPlaceable = measurables.first { it.layoutId == "contentFooter" }.measure(measureConstraints)
                    val contentPlaceable = measurables.first { it.layoutId == "content" }.measure(measureConstraints.copy(
                        maxHeight = constraints.maxHeight - footerPlaceable.height - contentFooterPlaceable.height)
                    )
                    collapsingHeight = collapsingPlaceable.height
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val swipeOffset = swipeableState.offset.value.roundToInt()
                        contentPlaceable.placeRelative(0, collapsingPlaceable.height + footerPlaceable.height + swipeOffset)
                        footerPlaceable.placeRelative(0, collapsingPlaceable.height + swipeOffset)
                        collapsingPlaceable.placeRelative(0, swipeOffset)
                        contentFooterPlaceable.placeRelative(0, constraints.maxHeight - contentFooterPlaceable.height)
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
