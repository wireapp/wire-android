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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
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
    val drawModifier = Modifier
        .fillMaxSize()
        .clipToBounds() // necessary to draw inside the canvas.
        .background(Color.White)
        .onSizeChanged { onSizeChanged(it.toSize()) }
        .pointerInput(Unit) { awaitEachGesture { handleGestures(onStartDrawing, onDraw, onStopDrawing) } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
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
        if (paths.isEmpty()) {
            EmptyCanvasHint(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun EmptyCanvasHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = dimensions().spacing16x)
            .width(IntrinsicSize.Max), // keeps column as wide as the text, so the arrow follows it on wide screens
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.sketch_details_empty_text),
            style = MaterialTheme.wireTypography.body01,
            color = colorsScheme().secondaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(30.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.fillMaxWidth(ARROW_START_OFFSET_FRACTION))
            // todo: change later this mirrored to the non mirrored, when we have the full toolbar.
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_onboarding_mirror),
                contentDescription = null,
            )
        }
    }
}

private const val ARROW_START_OFFSET_FRACTION = 0.25f

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
        val hasNewLineDraw = event.changes.first().positionChanged()
        if (hasNewLineDraw) {
            event.changes.first().consume()
        }
    } while (event.changes.any { it.pressed })
    onStopDrawing()
}

@Preview(showBackground = true, device = Devices.NEXUS_10)
@Preview(showBackground = true, device = Devices.PIXEL_2)
@Preview(showBackground = true, device = Devices.PIXEL_7A)
@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.PIXEL_2, locale = "de")
@Preview(showBackground = true, device = Devices.NEXUS_10, locale = "de")
@Preview(showBackground = true, locale = "de", fontScale = 2f)
@Preview(showBackground = true, widthDp = 800, heightDp = 360, locale = "de")
@Composable
fun PreviewCanvasEmptyState() {
    DrawingCanvasComponent(
        state = DrawingState(),
        onStartDrawingEvent = {},
        onDrawEvent = {},
        onStopDrawingEvent = {},
        onSizeChanged = {},
        onStartDrawing = {},
        onDraw = {},
        onStopDrawing = {}
    )
}
