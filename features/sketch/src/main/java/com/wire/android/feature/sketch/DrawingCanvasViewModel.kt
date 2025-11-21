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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.sketch.navArgs
import com.wire.android.feature.sketch.model.DrawingCanvasNavArgs
import com.wire.android.feature.sketch.model.DrawingMotionEvent
import com.wire.android.feature.sketch.model.DrawingPathProperties
import com.wire.android.feature.sketch.model.DrawingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("TooManyFunctions")
class DrawingCanvasViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val drawingCanvasNavArgs: DrawingCanvasNavArgs = savedStateHandle.navArgs()

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
     * Saves the image to the provided URI and resets the canvas.
     *
     * @param context The context to use to open the file descriptor.
     *
     * @return The [Uri] of the saved image.
     */
    suspend fun saveImage(context: Context): Uri {
        val tempSketchFile = drawingCanvasNavArgs.tempWritableUri.orTempUri(context)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                with(state) {
                    if (canvasSize == null || state.paths.isEmpty()) return@withContext

                    val bitmap = Bitmap.createBitmap(
                        canvasSize.width.toInt(),
                        canvasSize.height.toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap).apply { drawPaint(Paint().apply { color = Color.White.toArgb() }) }
                    context.contentResolver.openFileDescriptor(tempSketchFile, "rwt")?.use { fileDescriptor ->
                        FileOutputStream(fileDescriptor.fileDescriptor).use { fileOutputStream ->
                            paths.forEach { path -> path.drawNative(canvas) }
                            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fileOutputStream)
                            fileOutputStream.flush()
                        }.also {
                            Log.d("DrawingCanvasViewModel", "Image written to: $tempSketchFile")
                        }
                    }
                }
            }
        }.join()
        initializeCanvas()
        return tempSketchFile
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

    private fun Uri?.orTempUri(context: Context): Uri = this ?: run {
        val tempFile = File.createTempFile("temp_sketch", ".jpg", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.toUri()
    }

    companion object {
        private const val QUALITY = 50
    }
}
