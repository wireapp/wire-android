package com.wire.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
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
    suspend fun postProcessAvatar(uri: Uri): Uri? {
        try {
            val avatarByteArray = uri.toByteArray(context)

            // Compress image
            val resampledByteArray = avatarByteArray?.let { ImageUtil.resample(it, ImageUtil.ImageSizeClass.Small) }

            // Save to fixed path
            return resampledByteArray?.let { getWritableTempAvatarUri(it) }
        } catch (exception: Exception) {
            // NOOP: None post process op performed
        }

        return null
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

    fun getSharableTempAvatarUri(): Uri {
        return Companion.getSharableTempAvatarUri(context)
    }

    fun getSharableAvatarUri(): Uri {
        return Companion.getSharableAvatarUri(context)
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

        fun getSharableAvatarUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, context.getProviderAuthority(), getAvatarFile(context))
        }

        fun getSharableTempAvatarUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, context.getProviderAuthority(), getTempAvatarFile(context))
        }
    }
}
