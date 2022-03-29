package com.wire.android.util

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
const val DEFAULT_FILE_MIME_TYPE = "file/*"
const val IMAGE_COMPRESSION_RATIO = 75

class ImageUtil private constructor() {
    companion object {
        /**
         * Given an image [uri] rotates the image to a [ExifInterface.ORIENTATION_NORMAL] and overwrite the original un-rotated image
         * Also compress the data to have an efficient data management, without losing quality
         *
         * In case of failure, just use the same picture without post processing
         *
         * @param uri the image location on which the operation will be performed
         * @param context
         */
        @Suppress("TooGenericExceptionCaught")
        fun postProcessCapturedAvatar(uri: Uri, context: Context) {
            try {
                val avatarBitmap = uri.toBitmap(context)

                // Rotate if needed
                val exifInterface = context.contentResolver.openInputStream(uri).use { stream -> stream?.let { ExifInterface(it) } }
                val normalizedAvatar = avatarBitmap?.rotateImageToNormalOrientation(exifInterface)

                // Compress image
                val rawCompressedImage = normalizedAvatar?.let { compressImage(it) }

                // Save to fixed path
                rawCompressedImage?.let { getWritableTempAvatarUri(it, context) }
            } catch (exception: Exception) {
                // NOOP: None post process op performed
            }
        }

        /**
         * Compress image to save some disk space and memory
         */
        fun compressImage(imageBitmap: Bitmap): ByteArray? {
            val byteArrayOutputStream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_RATIO, byteArrayOutputStream)
            return byteArrayOutputStream.use { it.toByteArray() }
        }
    }
}

/**
 * Rotates the image to its [ExifInterface.ORIENTATION_NORMAL] in case it's rotated with a different orientation than landscape or portrait
 * See more about exif interface at: https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
 *
 * @param exif Exif interface for of the image to rotate
 * @return Bitmap the rotated bitmap or the same in case there is no rotation performed
 */
@Suppress("MagicNumber", "TooGenericExceptionCaught")
private fun Bitmap.rotateImageToNormalOrientation(exif: ExifInterface?): Bitmap {
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
