package com.wire.android.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

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

fun Context.getFileName(uri: Uri): String? = when(uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()

fun Context.logToFile() {
    this.getExternalFilesDir(null)?.let {
        val logDirectory = File("${externalCacheDir?.absolutePath}", "logs")
        if (!logDirectory.exists()) {
            logDirectory.mkdir()
        }

        val logFile = File(logDirectory, "log.txt")
        // clear the previous logcat and then write the new one to the file
        try {
            Runtime.getRuntime().exec("logcat -c -v raw")
            Runtime.getRuntime().exec("logcat -f $logFile")
        } catch (e: IOException) {
            appLogger.e("create Log file failed", e)
        }
    }
    this.startFileShareIntent()
}

private fun rename(from: File, to: File): Boolean {
    return from.parentFile.exists() && from.exists() && from.renameTo(to)
}

fun Context.startFileShareIntent() {
    val file = File(this.externalCacheDir?.absolutePath + "/logs/" + "log.txt")
    val fileURI = FileProvider.getUriForFile(
        this, this.packageName + ".provider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    shareIntent.putExtra(
        Intent.EXTRA_SUBJECT,
        "Sharing Log from Wire"
    )

    shareIntent.putExtra(Intent.EXTRA_STREAM, fileURI)
    shareIntent.type = fileURI.getMimeType(context = this)
    startActivity(shareIntent)
}

private const val TEMP_IMG_ATTACHMENT_FILENAME = "temp_img_attachment.jpg"
private const val TEMP_VIDEO_ATTACHMENT_FILENAME = "temp_video_attachment.mp4"
