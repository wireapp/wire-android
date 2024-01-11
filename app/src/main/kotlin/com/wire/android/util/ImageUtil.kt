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

package com.wire.android.util

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import okio.Path
import okio.buffer
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.round
import kotlin.math.sqrt

const val DEFAULT_FILE_MIME_TYPE = "file/*"

object ImageUtil {

    /**
     * The use size class of an image. Medium for images in conversations, small for Avatars
     */
    enum class ImageSizeClass {
        Medium, Small
    }

    /**
     * Attempts to read the width and height of an image represented by the input parameter
     */
    fun extractImageWidthAndHeight(kaliumFileSystem: KaliumFileSystem, imageDataPath: Path): Pair<Int, Int> {
        kaliumFileSystem.source(imageDataPath).buffer().use { bufferedSource ->
            BitmapFactory.decodeStream(bufferedSource.inputStream()).let { bitmap ->
                return bitmap.width to bitmap.height
            }
        }
    }

    /**
     * Resamples, downscales and normalizes rotation of an image based on its intended [ImageSizeClass] use.
     * Works on JPEGS Only.
     *
     * @param byteArray the ByteArray representing the image
     * @param sizeClass the indented size class use case
     * @return ByteArray the resampled, downscaled and rotation normalized image or the original image if there was no need for downscaling
     */
    fun resample(byteArray: ByteArray, sizeClass: ImageSizeClass): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        val targetDimension = dimensionForSizeClass(sizeClass)
        if (shouldScale(bitmap, targetDimension)) {
            val exifInterface = ExifInterface(byteArray.inputStream())
            val size = scaledSizeForBitmap(bitmap, targetDimension)
            val resizedImage = Bitmap
                .createScaledBitmap(bitmap, size.first.toInt(), size.second.toInt(), true)
                .rotateImageToNormalOrientation(exifInterface)
            val output = ByteArrayOutputStream()
            if (resizedImage.hasAlpha()) {
                resizedImage.compress(Bitmap.CompressFormat.PNG, 0, output)
            } else {
                resizedImage.compress(Bitmap.CompressFormat.JPEG, compressionFactorForSizeClass(sizeClass), output)
            }
            return output.toByteArray()
        }
        return byteArray
    }

    // region Private

    // We will not require scaling if the image is within 30% of the target size
    private const val SCALE_FUDGE_FACTOR = 1.3F

    @Suppress("MagicNumber")
    private fun dimensionForSizeClass(sizeClass: ImageSizeClass): Float {
        return when (sizeClass) {
            ImageSizeClass.Small -> 280F
            ImageSizeClass.Medium -> 1448F
        }
    }

    @Suppress("MagicNumber")
    private fun compressionFactorForSizeClass(sizeClass: ImageSizeClass): Int {
        return when (sizeClass) {
            ImageSizeClass.Small -> 75
            ImageSizeClass.Medium -> 45
        }
    }

    @Suppress("MagicNumber")
    private fun scaledSizeForBitmap(bitmap: Bitmap, targetDimension: Float): Pair<Float, Float> {
        val scale1 = kotlin.math.max(targetDimension / bitmap.width, targetDimension / bitmap.height)
        val scale2 = targetDimension / sqrt((bitmap.width * bitmap.height).toFloat())
        val scale: Float = if (scale2.isFinite() && (scale2 < scale1)) {
            0.5F * (scale2 + scale1)
        } else {
            scale1
        }
        val width = ceil(scale * bitmap.width)
        val height = round(width / bitmap.width * bitmap.height)
        return width to height
    }

    private fun shouldScale(bitmap: Bitmap, targetDimension: Float): Boolean {
        val oneSizeIsTooLong = (bitmap.width > SCALE_FUDGE_FACTOR * targetDimension)
                || (bitmap.height > SCALE_FUDGE_FACTOR * targetDimension)
        val maxPixelCount = SCALE_FUDGE_FACTOR * targetDimension * targetDimension
        val pixelCountIsTooBig = bitmap.width * bitmap.height > maxPixelCount
        return (oneSizeIsTooLong && pixelCountIsTooBig)
    }

    // endregion
}

/**
 * Converts a ByteArray into a Bitmap
 */
fun ByteArray.toBitmap(): Bitmap? = BitmapFactory.decodeByteArray(this, 0, this.size)

/**
 * Converts a Uri in the formats [SCHEME_CONTENT] or [SCHEME_FILE] into a Bitmap
 */
fun Uri.toBitmap(context: Context): Bitmap? {
    return when (scheme == SCHEME_CONTENT || scheme == SCHEME_FILE) {
        true -> context.contentResolver.openInputStream(this).use { stream -> BitmapFactory.decodeStream(stream) }
        false -> null // we don't want to convert app assets (ie: default avatar icon) into bitmap
    }
}

/**
 * Rotates the image to its [ExifInterface.ORIENTATION_NORMAL] in case it's rotated with a different orientation than
 * landscape or portrait See more about exif interface at:
 * https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
 *
 * @param exif Exif interface for of the image to rotate
 * @return Bitmap the rotated bitmap or the same in case there is no rotation performed
 */
@Suppress("MagicNumber", "TooGenericExceptionCaught")
fun Bitmap.rotateImageToNormalOrientation(exif: ExifInterface?): Bitmap {
    val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return this
    }

    return try {
        val rotated = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
        this.recycle()
        rotated
    } catch (exception: Exception) {
        this
    }
}
