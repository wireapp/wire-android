/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.util.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.Build.VERSION.SDK_INT
import android.os.ParcelFileDescriptor
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import java.io.File
import kotlin.math.roundToInt

class PdfPreviewDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode() =
        getPage(source.file().toFile()) { page ->

            val srcWidth = page.width
            val srcHeight = page.height

            val dstSize = if (srcWidth > 0 && srcHeight > 0) {
                val dstWidth = options.size.widthPx(options.scale) { srcWidth }
                val dstHeight = options.size.heightPx(options.scale) { srcHeight }
                val rawScale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = srcWidth,
                    srcHeight = srcHeight,
                    dstWidth = dstWidth,
                    dstHeight = dstHeight,
                    scale = options.scale
                )
                val scale = if (options.precision == Precision.INEXACT) {
                    rawScale.coerceAtMost(1.0)
                } else {
                    rawScale
                }
                val width = (scale * srcWidth).roundToInt()
                val height = (scale * srcHeight).roundToInt()
                Size(width, height)
            } else {
                Size(srcWidth, srcHeight)
            }

            val (dstWidth, dstHeight) = dstSize
            val rawBitmap = Bitmap.createBitmap(
                (dstWidth as Dimension.Pixels).px,
                (dstHeight as Dimension.Pixels).px,
                Bitmap.Config.ARGB_8888
            )
            rawBitmap.eraseColor(Color.WHITE)

            page.render(rawBitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY)

            val bitmap = normalizeBitmap(rawBitmap, options.size)

            val isSampled = if (page.width > 0 && page.height > 0) {
                DecodeUtils.computeSizeMultiplier(
                    srcWidth = page.width,
                    srcHeight = page.height,
                    dstWidth = bitmap.width,
                    dstHeight = bitmap.height,
                    scale = options.scale
                ) < 1.0
            } else {
                true
            }

            DecodeResult(
                image = rawBitmap.asImage(),
                isSampled = isSampled
            )
        }

    private fun <T> getPage(pdfFile: File, pageNumber: Int = 0, block: (Page) -> T): T =
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(pageNumber).use { page -> block(page) }
            }
        }

    private fun normalizeBitmap(inBitmap: Bitmap, size: Size): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = inBitmap.width,
            srcHeight = inBitmap.height,
            dstWidth = size.width.pxOrElse { inBitmap.width },
            dstHeight = size.height.pxOrElse { inBitmap.height },
            scale = options.scale
        ).toFloat()
        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()
        val safeConfig = when {
            SDK_INT >= android.os.Build.VERSION_CODES.O && options.bitmapConfig == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.bitmapConfig
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean {
        return SDK_INT < android.os.Build.VERSION_CODES.O ||
                bitmap.config != Bitmap.Config.HARDWARE ||
                options.bitmapConfig == Bitmap.Config.HARDWARE
    }

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        if (options.precision == Precision.INEXACT) return true
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale
        )
        return multiplier == 1.0
    }

    private inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
        return if (isOriginal) original() else width.toPx(scale)
    }

    private inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
        return if (isOriginal) original() else height.toPx(scale)
    }

    private fun Dimension.toPx(scale: Scale) = pxOrElse {
        when (scale) {
            Scale.FILL -> Int.MIN_VALUE
            Scale.FIT -> Int.MAX_VALUE
        }
    }
}
