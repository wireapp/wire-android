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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireTypography

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
    CanvasLayout(
        state = state,
        onStartDrawingEvent = onStartDrawingEvent,
        onDrawEvent = onDrawEvent,
        onStopDrawingEvent = onStopDrawingEvent,
        onSizeChanged = onSizeChanged,
        onStartDrawing = onStartDrawing,
        onDraw = onDraw,
        onStopDrawing = onStopDrawing
    )
}

@Composable
private fun CanvasLayout(
    state: DrawingState,
    onStartDrawingEvent: () -> Unit,
    onDrawEvent: () -> Unit,
    onStopDrawingEvent: () -> Unit,
    onSizeChanged: (Size) -> Unit,
    onStartDrawing: (Offset) -> Unit,
    onDraw: (Offset) -> Unit,
    onStopDrawing: () -> Unit
) = with(state) {
    val textMeasurer = rememberTextMeasurer()
    val emptyCanvasText = stringResource(id = R.string.sketch_details_empty_text)
    val emptyCanvasStyle = MaterialTheme.wireTypography.body01.copy(color = colorsScheme().secondaryText)
    val textLayoutResult = remember(emptyCanvasText) {
        textMeasurer.measure(emptyCanvasText, emptyCanvasStyle)
    }
    val vector = ImageVector.vectorResource(id = R.drawable.ic_long_arrow)
    val painter = rememberVectorPainter(image = vector)
    val drawModifier = Modifier
        .fillMaxSize()
        .clipToBounds() // necessary to draw inside the canvas.
        .background(Color.White)
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
        if (paths.isEmpty()) {
            emptyCanvasState(textMeasurer, emptyCanvasText, emptyCanvasStyle, textLayoutResult, painter)
        }
    }
}

private fun DrawScope.emptyCanvasState(
    textMeasurer: TextMeasurer,
    emptyCanvasText: String,
    emptyCanvasStyle: TextStyle,
    textLayoutResult: TextLayoutResult,
    painter: VectorPainter
) {
    drawText(
        textMeasurer = textMeasurer,
        text = emptyCanvasText,
        style = emptyCanvasStyle,
        topLeft = Offset(
            x = center.x - textLayoutResult.size.width / 2,
            y = center.y - textLayoutResult.size.height / 2
        )
    )
    // todo. uncomment when figure it out how to position this correctly on the canvas.
    val enabled = false
    if (enabled) {
        with(painter) {
            draw(painter.intrinsicSize, alpha = .5f)
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
