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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.wire.android.feature.sketch.model.DrawingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal interface SketchImageSaver {
    suspend fun save(state: DrawingState, tempWritableUri: Uri?): Uri
}

internal class AndroidSketchImageSaver(private val context: Context) : SketchImageSaver {

    override suspend fun save(state: DrawingState, tempWritableUri: Uri?): Uri = withContext(Dispatchers.IO) {
        val tempSketchFile = tempWritableUri.orTempUri(context)
        with(state) {
            if (canvasSize == null || paths.isEmpty()) return@withContext tempSketchFile

            val bitmap = Bitmap.createBitmap(
                canvasSize.width.toInt(),
                canvasSize.height.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap).apply {
                drawPaint(Paint().apply { color = Color.White.toArgb() })
            }
            context.contentResolver.openFileDescriptor(tempSketchFile, "rwt")?.use { fileDescriptor ->
                FileOutputStream(fileDescriptor.fileDescriptor).use { fileOutputStream ->
                    paths.forEach { path -> path.drawNative(canvas) }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fileOutputStream)
                    fileOutputStream.flush()
                }.also {
                    Log.d(TAG, "Image written to: $tempSketchFile")
                }
            }
        }
        tempSketchFile
    }

    private fun Uri?.orTempUri(context: Context): Uri = this ?: run {
        val tempFile = File.createTempFile("temp_sketch", ".jpg", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.toUri()
    }

    private companion object {
        const val QUALITY = 50
        const val TAG = "AndroidSketchImageSaver"
    }
}
