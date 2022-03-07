package com.wire.android.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import java.io.File

fun saveAvatarToInternalStorage(avatarUri: Uri, context: Context): Boolean {
    val defaultAvatarDir = getAvatarFile(context)
    try {
        context.applicationContext.contentResolver.openInputStream(avatarUri).use { input ->
            defaultAvatarDir.outputStream().use { output ->
                input?.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return true
}

fun getTempAvatarUri(context: Context): Uri =
    Uri.parse(File(context.filesDir, AVATAR_PATH).path)

private fun getAvatarFile(context: Context): File {
    val newDir = File(context.filesDir, AVATAR_PATH)
    val pFile = newDir.parentFile
    pFile?.mkdirs()

    return newDir
}

/**
 * Gets the uri of any drawable or given resource
 * @param context - context
 * @param drawableId - drawable res id
 * @return - uri
 */
fun getUriToDrawable(
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

private const val AVATAR_PATH = "temp_avatar_path.jpg"
