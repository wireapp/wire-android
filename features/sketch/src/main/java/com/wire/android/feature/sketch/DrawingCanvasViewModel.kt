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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.feature.sketch.model.MotionEvent
import com.wire.android.feature.sketch.model.PathProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DrawingCanvasViewModel : ViewModel() {

    var state: DrawingState by mutableStateOf(DrawingState())
        private set

    /**
     * Marks the start of the drawing.
     */
    fun onStartDrawing(offset: Offset) {
        state = state.copy(currentPosition = offset, motionEvent = MotionEvent.Down)
    }

    /**
     * Marks the drawing in progress.
     */
    fun onDraw(offset: Offset) {
        state = state.copy(currentPosition = offset, motionEvent = MotionEvent.Move)
    }

    /**
     * Marks the end of the drawing.
     */
    fun onStopDrawing() {
        state = state.copy(motionEvent = MotionEvent.Up)
    }

    /**
     * Stores the initial point of the drawing.
     */
    fun onStartDrawingEvent() {
        state = state.copy(paths = state.paths + state.currentPath).apply {
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
            currentPath = PathProperties().apply {
                strokeWidth = state.currentPath.strokeWidth
                color = state.currentPath.color
                drawMode = state.currentPath.drawMode
            }, pathsUndone = emptyList(),
            currentPosition = Offset.Unspecified,
            motionEvent = MotionEvent.Idle
        )
    }

    /**
     * Sets the canvas size or modifies it if zoom is implemented.
     */
    fun onSizeChanged(canvasSize: Size) {
        state = state.copy(canvasSize = canvasSize)
    }

    fun saveImage(tempWritableImageUri: Uri?): Uri {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                with(state) {
                    if (canvasSize == null || state.paths.isEmpty()) return@withContext

                    val bitmap = Bitmap.createBitmap(canvasSize!!.width.toInt(), canvasSize!!.height.toInt(), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap).apply { drawPaint(Paint().apply { color = Color.WHITE }) }
                    val file = File(getImagePath(tempWritableImageUri))
                    val outputStream = FileOutputStream(file, false)
                    outputStream.use {
                        paths.forEach { path -> path.drawNative(canvas) }
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    outputStream.flush()
                    outputStream.close()
                }
            }
        }
        return getImagePath(tempWritableImageUri).toUri()
    }

    private fun getImagePath(tempWritableImageUri: Uri?): String {
        return Environment.getDownloadCacheDirectory().path + "/$TEMP_IMG_ATTACHMENT_FILENAME"
    }

    companion object {
        private const val TEMP_IMG_ATTACHMENT_FILENAME = "image_attachment.jpg"
    }

}
