package com.wire.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import okio.Path
import javax.inject.Inject

class AvatarImageManager @Inject constructor(val context: Context) {

    fun getWritableAvatarUri(imageDataPath: Path): Uri {
        val file = imageDataPath.toFile()
        return file.toUri()
    }

    fun getShareableTempAvatarUri(filePath: Path): Uri {
        return FileProvider.getUriForFile(context, context.getProviderAuthority(), filePath.toFile())
    }
}
