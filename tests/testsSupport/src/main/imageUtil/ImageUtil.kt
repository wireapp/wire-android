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
import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.min

object ImageUtil {

    fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    fun scaleTo(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scaling ratio while maintaining aspect ratio
        val widthRatio = maxWidth.toFloat() / width
        val heightRatio = maxHeight.toFloat() / height
        val scaleRatio = min(widthRatio, heightRatio).coerceAtMost(1f) // Don't scale up

        // Create matrix for scaling
        val matrix = Matrix()
        matrix.postScale(scaleRatio, scaleRatio)

        // Create scaled bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    @Throws(IOException::class)
    fun asByteArray(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.WEBP, quality: Int = 80): ByteArray {
        ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(format, quality, outputStream)
            return outputStream.toByteArray()
        }
    }
}
