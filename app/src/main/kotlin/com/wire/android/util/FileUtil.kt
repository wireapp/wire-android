package com.wire.android.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.wire.android.BuildConfig
import com.wire.android.R
import java.io.File

/**
 * Gets the uri of any drawable or given resource
 * @param context - context
 * @param drawableId - drawable res id
 * @return - uri
 */
fun getUriFromDrawable(
    @NonNull context: Context,
    @AnyRes drawableId: Int
): Uri {
    return Uri.parse(
        ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + context.resources.getResourcePackageName(drawableId)
                + '/' + context.resources.getResourceTypeName(drawableId)
                + '/' + context.resources.getResourceEntryName(drawableId)
    )
}
@Suppress("MagicNumber")
fun Uri.toByteArray(context: Context): ByteArray {
    return context.contentResolver.openInputStream(this)?.readBytes() ?: ByteArray(16)
}

fun getShareableAvatarUri(context: Context): Uri {
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", getTempAvatarFile(context))
}

fun getTempAvatarUri(imageData: ByteArray, context: Context): Uri {
    val file = getTempAvatarFile(context)
    file.writeBytes(imageData)
    return file.toUri()
}

private fun getTempAvatarFile(context: Context): File {
    val file = File(context.cacheDir, TEMP_AVATAR_FILENAME)
    file.setWritable(true, false)
    return file
}

fun getDefaultAvatarUri(context: Context): Uri {
    return getUriFromDrawable(context, R.drawable.ic_launcher_foreground)
}

private const val TEMP_AVATAR_FILENAME = "temp_avatar_path.jpg"
