package com.wire.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.wire.android.BuildConfig
import java.io.File
import javax.inject.Inject

class AvatarImageManager @Inject constructor(val context: Context) {

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
    fun postProcessCapturedAvatar(uri: Uri) {
        try {
            val avatarBitmap = uri.toBitmap(context)

            // Rotate if needed
            val exifInterface = context.contentResolver.openInputStream(uri).use { stream -> stream?.let { ExifInterface(it) } }
            val normalizedAvatar = avatarBitmap?.rotateImageToNormalOrientation(exifInterface)

            // Compress image
            val rawCompressedImage = normalizedAvatar?.let { ImageUtil.compressImage(it) }

            // Save to fixed path
            rawCompressedImage?.let { getWritableTempAvatarUri(it) }
        } catch (exception: Exception) {
            // NOOP: None post process op performed
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

    private fun getWritableTempAvatarUri(imageData: ByteArray): Uri {
        val file = getTempAvatarFile(context)
        file.writeBytes(imageData)
        return file.toUri()
    }

    fun getWritableAvatarUri(imageData: ByteArray): Uri {
        val file = getAvatarFile(context)
        file.writeBytes(imageData)
        return file.toUri()
    }

    suspend fun uriToByteArray(uri: Uri): ByteArray {
        return uri.toByteArray(context)
    }

    fun getShareableTempAvatarUri(): Uri {
        return Companion.getShareableTempAvatarUri(context)
    }

    companion object {
        private const val TEMP_AVATAR_FILENAME = "temp_avatar_path.jpg"
        private const val AVATAR_FILENAME = "user_avatar_path.jpg"

        fun getTempAvatarFile(context: Context): File {
            val file = File(context.cacheDir, TEMP_AVATAR_FILENAME)
            file.setWritable(true, false)
            return file
        }

        fun getAvatarFile(context: Context): File {
            val file = File(context.cacheDir, AVATAR_FILENAME)
            file.setWritable(true, false)
            return file
        }

        fun getShareableAvatarUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, context.getProviderAuthority(), getAvatarFile(context))
        }

        fun getShareableTempAvatarUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, context.getProviderAuthority(), getTempAvatarFile(context))
        }
    }
}
