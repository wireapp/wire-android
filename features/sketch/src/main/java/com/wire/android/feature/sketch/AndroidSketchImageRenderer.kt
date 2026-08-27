/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.sketch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.content.media.EncodedImage
import dev.zacsweers.metro.Inject
import java.io.ByteArrayOutputStream

internal fun interface SketchImageRenderer {
    fun render(state: DrawingState): EncodedImage?
}

@Inject
internal class AndroidSketchImageRenderer : SketchImageRenderer {
    override fun render(state: DrawingState): EncodedImage? {
        val canvasSize = state.canvasSize
        if (canvasSize == null || state.paths.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(
            canvasSize.width.toInt(),
            canvasSize.height.toInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap).apply {
            drawPaint(Paint().apply { color = Color.White.toArgb() })
        }
        state.paths.forEach { path -> path.drawNative(canvas) }
        val output = ByteArrayOutputStream()
        return if (bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)) {
            EncodedImage(output.toByteArray(), JPEG_MIME_TYPE)
        } else {
            null
        }
    }

    private companion object {
        const val QUALITY = 50
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}
