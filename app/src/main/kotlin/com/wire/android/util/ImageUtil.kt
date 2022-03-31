package com.wire.android.util

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

const val DEFAULT_IMAGE_MIME_TYPE = "image/jpeg"
const val DEFAULT_FILE_MIME_TYPE = "file/*"
const val IMAGE_COMPRESSION_RATIO = 75

class ImageUtil private constructor() {
    companion object {

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
