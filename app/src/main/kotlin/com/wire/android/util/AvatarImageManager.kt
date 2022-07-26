package com.wire.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import okio.Path
import java.io.File
import javax.inject.Inject
import okio.Path.Companion.toOkioPath

class AvatarImageManager @Inject constructor(val context: Context) {

    fun getWritableAvatarUri(imageDataPath: Path): Uri {
        val file = imageDataPath.toFile()
        return file.toUri()
    }

    fun getShareableTempAvatarUri(): Uri {
        return getShareableAvatarUri(context)
    }

    companion object {
        private const val AVATAR_FILENAME = "user_avatar_path.jpg"

        private fun getAvatarFile(context: Context): File {
            val file = File(context.cacheDir, AVATAR_FILENAME)
            file.setWritable(true, false)
            return file
        }

        private fun getShareableAvatarUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, context.getProviderAuthority(), getAvatarFile(context))
        }
    }
}
