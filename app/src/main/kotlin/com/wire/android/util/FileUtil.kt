package com.wire.android.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.wire.android.BuildConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            "://" + context.resources.getResourcePackageName(drawableId) +
            '/' + context.resources.getResourceTypeName(drawableId) +
            '/' + context.resources.getResourceEntryName(drawableId)
    )
}

@Suppress("MagicNumber")
suspend fun Uri.toByteArray(context: Context): ByteArray {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(this@toByteArray)?.use { it.readBytes() } ?: ByteArray(16)
    }
}

fun getWritableImageAttachment(context: Context) = getTempWritableAttachmentUri(context, TEMP_IMG_ATTACHMENT_FILENAME)

fun getWritableVideoAttachment(context: Context) = getTempWritableAttachmentUri(context, TEMP_VIDEO_ATTACHMENT_FILENAME)

private fun getTempWritableAttachmentUri(context: Context, fileName: String): Uri {
    val file = File(context.cacheDir, fileName)
    file.setWritable(true)
    return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)
}

fun Uri.getMimeType(context: Context): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(path)
    return context.contentResolver.getType(this)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

private const val TEMP_IMG_ATTACHMENT_FILENAME = "temp_img_attachment.jpg"
private const val TEMP_VIDEO_ATTACHMENT_FILENAME = "temp_video_attachment.mp4"
