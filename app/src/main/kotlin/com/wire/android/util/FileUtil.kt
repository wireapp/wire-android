package com.wire.android.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.wire.android.BuildConfig
import com.wire.android.R
import java.io.File

fun getTempAvatarUri(context: Context): Uri {
    val file = File(context.cacheDir, AVATAR_PATH)
    file.setWritable(true, false)
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
}

fun Uri.toByteArray(context: Context): ByteArray {
    return context.contentResolver.openInputStream(this)?.readBytes() ?: ByteArray(16)
}

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

fun getDefaultAvatarUri(context: Context): Uri {
    return getUriFromDrawable(context, R.drawable.ic_launcher_foreground)
}

private const val AVATAR_PATH = "temp_avatar_path.jpg"
