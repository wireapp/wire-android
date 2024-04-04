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
package com.wire.android.feature.sketch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import com.wire.android.feature.sketch.model.DrawingState

@Composable
internal fun DrawingCanvasComponent(
    state: DrawingState,
    onStartDrawingEvent: () -> Unit,
    onDrawEvent: () -> Unit,
    onStopDrawingEvent: () -> Unit,
    onSizeChanged: (Size) -> Unit,
    onStartDrawing: (Offset) -> Unit,
    onDraw: (Offset) -> Unit,
    onStopDrawing: () -> Unit
) {
    with(state) {
        val drawModifier = Modifier
            .fillMaxSize()
            .clipToBounds() // necessary to draw inside the canvas.
            .background(MaterialTheme.colorScheme.surface)
            .onSizeChanged { onSizeChanged(it.toSize()) }
            .pointerInput(Unit) { awaitEachGesture { handleGestures(onStartDrawing, onDraw, onStopDrawing) } }
        Canvas(modifier = drawModifier) {
            with(drawContext.canvas.nativeCanvas) {
                val checkPoint = saveLayer(null, null)
                when (drawingMotionEvent) {
                    DrawingMotionEvent.Idle -> Unit
                    DrawingMotionEvent.Down -> onStartDrawingEvent()
                    DrawingMotionEvent.Move -> {
                        onDrawEvent()
                        // todo: draw with selected properties, out of scope for this first ticket.
                        drawCircle(
                            center = currentPosition,
                            color = Color.Gray,
                            radius = currentPath.strokeWidth / 2,
                            style = Stroke(width = 1f)
                        )
                    }

                    DrawingMotionEvent.Up -> onStopDrawingEvent()
                }
                paths.forEach { path ->
                    path.draw(this@Canvas /*, bitmap*/)
                }
                restoreToCount(checkPoint)
            }
        }
    }
}

/**
 * Handles the drawing gestures in the canvas.
 */
private suspend fun AwaitPointerEventScope.handleGestures(
    onStartDrawing: (Offset) -> Unit,
    onDraw: (Offset) -> Unit,
    onStopDrawing: () -> Unit
) {
    val downEvent = awaitFirstDown()
    onStartDrawing(downEvent.position)
    if (downEvent.pressed != downEvent.previousPressed) {
        downEvent.consume()
    }
    do {
        val event = awaitPointerEvent()
        onDraw(event.changes.first().position)
        val hasNewLineDraw = event.changes.first().positionChange() != Offset.Zero
        if (hasNewLineDraw) {
            event.changes.first().consume()
        }
    } while (event.changes.any { it.pressed })
    onStopDrawing()
}
