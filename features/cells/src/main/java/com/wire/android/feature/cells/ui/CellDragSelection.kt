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
package com.wire.android.feature.cells.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.paging.compose.LazyPagingItems
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.ui.common.dimensions
import kotlinx.coroutines.flow.collectLatest

/**
 * Long press drag selection: after a long press on an item, dragging up or down reports every item between it and the one
 * under the finger. The list auto scrolls while the finger is close to its top or bottom edge.
 *
 * Only the drag is handled here: the long press itself is the item's own long click, which toggles the pressed item.
 */
@Stable
internal class CellDragSelectionState(
    private val lazyListState: LazyListState,
    private val cellNodes: State<LazyPagingItems<CellNodeUi>>,
    private val onDragSelection: State<(List<CellNodeUi>) -> Unit>,
    private val onDragSelectionEnd: State<() -> Unit>,
    private val autoScrollEdgePx: Float,
) {
    // True from the long press until the finger is lifted.
    private var isDragging = false

    // Positive scrolls down, negative up, in px per frame.
    private var autoScrollSpeed by mutableFloatStateOf(0f)

    private var anchorIndex: Int? = null
    private var lastIndex: Int? = null
    private var pointerY = 0f

    /** @return false when there is no item under [offset] to start the drag from. */
    internal fun onDragStart(offset: Offset): Boolean {
        val index = itemIndexAt(offset.y) ?: return false
        anchorIndex = index
        lastIndex = index
        pointerY = offset.y
        isDragging = true
        return true
    }

    internal fun onDrag(pointerY: Float, viewportHeight: Int) {
        if (!isDragging) return
        this.pointerY = pointerY
        autoScrollSpeed = when {
            pointerY < autoScrollEdgePx -> -autoScrollSpeedFor(autoScrollEdgePx - pointerY)
            pointerY > viewportHeight - autoScrollEdgePx -> autoScrollSpeedFor(pointerY - (viewportHeight - autoScrollEdgePx))
            else -> 0f
        }
        updateSelection()
    }

    internal fun onDragEnd() {
        if (!isDragging) return
        isDragging = false
        autoScrollSpeed = 0f
        anchorIndex = null
        lastIndex = null
        onDragSelectionEnd.value()
    }

    internal suspend fun runAutoScroll() {
        snapshotFlow { autoScrollSpeed }.collectLatest { speed ->
            if (speed == 0f) return@collectLatest
            while (true) {
                lazyListState.scrollBy(speed)
                // The finger didn't move but the list did, so another item may now be under it.
                updateSelection()
                withFrameNanos { }
            }
        }
    }

    // The closer to the edge, the faster; beyond the edge (finger outside the list) at full speed.
    private fun autoScrollSpeedFor(distanceIntoEdge: Float): Float =
        (distanceIntoEdge / autoScrollEdgePx).coerceIn(0f, 1f) * autoScrollEdgePx * MAX_AUTO_SCROLL_SPEED_RATIO

    private fun updateSelection() {
        val anchor = anchorIndex ?: return
        // Clamped so that with the finger past the edge (auto scrolling), the selection follows the edge item.
        val viewport = lazyListState.layoutInfo
        if (viewport.viewportEndOffset <= viewport.viewportStartOffset) return
        val y =pointerY.coerceIn(viewport.viewportStartOffset.toFloat(), viewport.viewportEndOffset - 1f)
        val index = itemIndexAt(y) ?: return
        if (index == lastIndex) return
        lastIndex = index
        val range = if (index >= anchor) anchor..index else anchor downTo index
        // peek, not get: reading the range must not trigger page loads.
        onDragSelection.value(range.mapNotNull { cellNodes.value.peek(it) })
    }

    // Index of the node under [y], null for the gaps (footer, bottom padding) where no node is.
    private fun itemIndexAt(y: Float): Int? = lazyListState.layoutInfo.visibleItemsInfo
        .firstOrNull { y >= it.offset && y < it.offset + it.size }
        ?.index
        ?.takeIf { it < cellNodes.value.itemCount }

    private companion object {
        // A quarter of the edge zone per frame at full speed, ~ 1000dp/s at 60fps.
        const val MAX_AUTO_SCROLL_SPEED_RATIO = 0.25f
    }
}

@Composable
internal fun rememberCellDragSelectionState(
    lazyListState: LazyListState,
    cellNodes: LazyPagingItems<CellNodeUi>,
    onDragSelection: (List<CellNodeUi>) -> Unit,
    onDragSelectionEnd: () -> Unit,
): CellDragSelectionState {
    val currentCellNodes = rememberUpdatedState(cellNodes)
    val currentOnDragSelection = rememberUpdatedState(onDragSelection)
    val currentOnDragSelectionEnd = rememberUpdatedState(onDragSelectionEnd)
    val autoScrollEdgePx = with(LocalDensity.current) { dimensions().spacing64x.toPx() }
    val state = remember(lazyListState, autoScrollEdgePx) {
        CellDragSelectionState(
            lazyListState = lazyListState,
            cellNodes = currentCellNodes,
            onDragSelection = currentOnDragSelection,
            onDragSelectionEnd = currentOnDragSelectionEnd,
            autoScrollEdgePx = autoScrollEdgePx,
        )
    }
    LaunchedEffect(state) { state.runAutoScroll() }
    return state
}

/**
 * Not built on detectDragGesturesAfterLongPress: after its long click the item consumes every move, which that detector
 * takes as a cancellation. Instead, once the long press is detected, the moves are read and consumed in the Initial pass,
 * before the item and the list see them, which also keeps the list from scrolling along with the finger.
 *
 * @param state null when selection isn't available, which turns the gesture off.
 */
internal fun Modifier.cellDragSelection(state: CellDragSelectionState?): Modifier =
    if (state == null) {
        this
    } else {
        pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // Null if lifted before the timeout or if the list started scrolling (it consumes the moves).
                val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                if (!state.onDragStart(longPress.position)) return@awaitEachGesture
                try {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes
                            .firstOrNull { it.id == longPress.id }
                        if (change == null || !change.pressed) break
                        if (change.positionChange() != Offset.Zero) change.consume()
                        state.onDrag(change.position.y, size.height)
                    }
                } finally {
                    // Also runs when the gesture is removed mid drag (e.g. selection got disabled), to stop auto scrolling.
                    state.onDragEnd()
                }
            }
        }
    }
