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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.feature.sketch.model.MotionEvent
import com.wire.android.feature.sketch.model.PathProperties

@Composable
fun DrawingCanvasComponent(
    viewModel: DrawingCanvasViewModel = viewModel()
) {
    val drawModifier = Modifier
        .fillMaxSize()
        .clipToBounds() // necessary to draw in the canvas.
        .background(MaterialTheme.colorScheme.background)
        .pointerInput(Unit) {
            awaitEachGesture {
                val downEvent = awaitFirstDown()
                viewModel.currentPosition = downEvent.position - viewModel.canvasState.translation
                viewModel.motionEvent = MotionEvent.Down
                if (downEvent.pressed != downEvent.previousPressed) downEvent.consume()
                do {
                    val event = awaitPointerEvent()
                    viewModel.currentPosition = event.changes[0].position - viewModel.canvasState.translation
                    viewModel.motionEvent = MotionEvent.Move
                    if (event.changes[0].positionChange() != Offset.Zero) event.changes[0].consume()
                } while (event.changes.any { it.pressed })
                viewModel.motionEvent = MotionEvent.Up
            }
        }
    Canvas(modifier = drawModifier) {
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            when (viewModel.motionEvent) {
                MotionEvent.Idle -> Unit
                MotionEvent.Down -> {
                    viewModel.paths.add(viewModel.currentPath)
                    viewModel.currentPath.path.moveTo(
                        viewModel.currentPosition.x,
                        viewModel.currentPosition.y
                    )
                }

                MotionEvent.Move -> {
                    viewModel.currentPath.path.lineTo(
                        viewModel.currentPosition.x,
                        viewModel.currentPosition.y
                    )
                    drawCircle(
                        center = viewModel.currentPosition,
                        color = Color.Gray,
                        radius = viewModel.currentPath.strokeWidth / 2,
                        style = Stroke(
                            width = 1f
                        )
                    )
                }

                MotionEvent.Up -> {
                    viewModel.currentPath.path.lineTo(
                        viewModel.currentPosition.x,
                        viewModel.currentPosition.y
                    )
                    viewModel.currentPath = PathProperties(
                        path = Path(),
                        strokeWidth = viewModel.currentPath.strokeWidth,
                        color = viewModel.currentPath.color,
                        drawMode = viewModel.currentPath.drawMode
                    )
                    viewModel.pathsUndone.clear()
                    viewModel.currentPosition = Offset.Unspecified
                    viewModel.motionEvent = MotionEvent.Idle
                }
            }
            viewModel.paths.forEach { path ->
                path.draw(this@Canvas /*, bitmap*/)
            }
            restoreToCount(checkPoint)
        }
    }

}

