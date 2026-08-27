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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import com.wire.android.feature.sketch.model.DrawingPathProperties
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.media.EncodedImageExportRequest
import com.wire.content.media.EncodedImageExporter
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Suppress("TooManyFunctions")
class DrawingCanvasViewModel internal constructor(
    private val imageRenderer: SketchImageRenderer,
    private val imageExporter: EncodedImageExporter,
    private val kaliumFileSystem: KaliumFileSystem,
) : ViewModel() {

    internal var state: DrawingState by mutableStateOf(DrawingState())
        private set

    init {
        initializeCanvas()
    }

    fun initializeCanvas() {
        state = DrawingState(currentPath = DrawingPathProperties())
    }

    fun onShowConfirmationDialog() {
        state = state.copy(showConfirmationDialog = true)
    }

    fun onHideConfirmationDialog() {
        state = state.copy(showConfirmationDialog = false)
    }

    /**
     * Marks the start of the drawing.
     */
    fun onStartDrawing(offset: Offset) {
        state = state.copy(currentPosition = offset, drawingMotionEvent = DrawingMotionEvent.Down)
    }

    /**
     * Marks the drawing in progress.
     */
    fun onDraw(offset: Offset) {
        state = state.copy(currentPosition = offset, drawingMotionEvent = DrawingMotionEvent.Move)
    }

    /**
     * Marks the end of the drawing.
     */
    fun onStopDrawing() {
        state = state.copy(drawingMotionEvent = DrawingMotionEvent.Up)
    }

    /**
     * Stores the initial point of the drawing.
     */
    fun onStartDrawingEvent() {
        state = state.copy(paths = (state.paths + state.currentPath).toPersistentList()).apply {
            currentPath.path.moveTo(state.currentPosition.x, state.currentPosition.y)
        }
    }

    /**
     * Stores the drawing in progress.
     */
    fun onDrawEvent() {
        state.currentPath.path.lineTo(state.currentPosition.x, state.currentPosition.y)
    }

    /**
     * Stores the end point of the drawing and performs the necessary cleanup.
     */
    fun onStopDrawingEvent() {
        state.currentPath.path.lineTo(state.currentPosition.x, state.currentPosition.y)
        state = state.copy(
            currentPath = DrawingPathProperties().apply {
                strokeWidth = state.currentPath.strokeWidth
                color = state.currentPath.color
                drawMode = state.currentPath.drawMode
            },
            pathsUndone = persistentListOf(),
            currentPosition = Offset.Unspecified,
            drawingMotionEvent = DrawingMotionEvent.Idle
        )
    }

    /**
     * Sets the canvas size or modifies it if zoom is implemented.
     */
    fun onSizeChanged(canvasSize: Size) {
        state = state.copy(canvasSize = canvasSize)
    }

    /**
     * Undoes the last stroke.
     */
    fun onUndoLastStroke() {
        if (state.paths.isNotEmpty()) {
            state = state.copy(
                paths = state.paths.distinct().dropLast(1).toPersistentList(),
                pathsUndone = (state.pathsUndone + state.paths.last()).toPersistentList()
            )
        }
    }

    /**
     * Renders the sketch through the Android adapter, exports it through the common boundary,
     * and resets the canvas.
     */
    suspend fun saveImage(destination: ExternalContentReference?): PlatformResult<ExternalContentReference> {
        val image = imageRenderer.render(state)
        val result = if (image == null) {
            PlatformResult.Cancelled
        } else {
            imageExporter.export(
                EncodedImageExportRequest(
                    image = image,
                    displayName = TEMP_SKETCH_FILENAME,
                    fallbackPath = kaliumFileSystem.rootCachePath / TEMP_SKETCH_FILENAME,
                    destination = destination,
                )
            )
        }
        initializeCanvas()
        return result
    }

    fun onColorChanged(selectedColor: Color) {
        state = state.copy(
            currentPath = DrawingPathProperties().apply {
                strokeWidth = state.currentPath.strokeWidth
                color = selectedColor
                drawMode = state.currentPath.drawMode
            }
        )
    }

    companion object {
        private const val TEMP_SKETCH_FILENAME = "temp_sketch.jpg"
    }
}
