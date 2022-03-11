package com.wire.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.wire.android.util.extension.toByteArray

/**
 * Rotates the image to a [ExifInterface.ORIENTATION_NORMAL] in case it's rotated with a different orientation
 *
 * @param exif Exif interface for of the image to rotate
 * @return Bitmap the rotated bitmap or the same in case there is no rotation performed
 */
private fun Bitmap.rotateImageToNormalOrientation(exif: ExifInterface): Bitmap {
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> return this
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
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
 * Given an image [uri] rotates the image to a [ExifInterface.ORIENTATION_NORMAL] and overwrite the original un-rotated image
 *
 * @param uri the image location on which the operation will be performed
 * @param context
 */
fun rotateImageIfNeeded(uri: Uri, context: Context) {
    val rawImage = uri.toByteArray(context)
    val avatarBitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.size)

    val normalizedAvatar = avatarBitmap.rotateImageToNormalOrientation(ExifInterface(uri.toInputStream(context)!!))
    if (normalizedAvatar != avatarBitmap) {
        getTempAvatarUri(normalizedAvatar.toByteArray(), context)
    }
}
