@file:Suppress("TooManyFunctions")

package com.wire.android.util

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.wire.android.appLogger
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.buffer
import java.io.File
import java.io.InputStream

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

fun Context.getTempWritableImageUri() = getTempWritableAttachmentUri(this, TEMP_IMG_ATTACHMENT_FILENAME)
fun Context.getTempWritableVideoUri() = getTempWritableAttachmentUri(this, TEMP_VIDEO_ATTACHMENT_FILENAME)

private fun getTempWritableAttachmentUri(context: Context, fileName: String): Uri {
    val file = File(context.cacheDir, fileName)
    file.setWritable(true)
    return FileProvider.getUriForFile(context, context.getProviderAuthority(), file)
}

private fun Context.saveFileDataToDownloadsFolder(downloadedDataPath: Path, fileSize: Long, kaliumFileSystem: KaliumFileSystem): Uri? {
    val resolver = contentResolver
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, downloadedDataPath.name)
            put(MediaStore.MediaColumns.MIME_TYPE, Uri.parse(downloadedDataPath.name).getMimeType(this@saveFileDataToDownloadsFolder))
            put(MediaStore.MediaColumns.SIZE, fileSize)
        }
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val authority = getProviderAuthority()
        val destinationFile = File(getExternalFilesDir(DIRECTORY_DOWNLOADS), downloadedDataPath.name)
        FileProvider.getUriForFile(this, authority, destinationFile)
    }?.also { downloadedUri ->
        resolver.openOutputStream(downloadedUri).use { outputStream ->
            val brr = ByteArray(DATA_COPY_BUFFER_SIZE)
            var len: Int
            val bufferedInputStream: InputStream = kaliumFileSystem.source(downloadedDataPath).buffer().inputStream()
            while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                outputStream?.write(brr, 0, len)
            }
            outputStream?.flush()
            bufferedInputStream.close()
        }
    }
}

fun Context.copyDataToTempShareableFile(assetName: String, assetDataPath: Path, kaliumFileSystem: KaliumFileSystem): Uri {
    val tempOutputFile = File(cacheDir, assetName)
    tempOutputFile.setWritable(true)

    // Copy asset data to temp file
    kaliumFileSystem.writeData(tempOutputFile.toOkioPath(), kaliumFileSystem.source(assetDataPath))
    return FileProvider.getUriForFile(this, getProviderAuthority(), tempOutputFile)
}

fun Uri.getMimeType(context: Context): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(path)
    return context.contentResolver.getType(this)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}

fun Uri.copyToTempPath(context: Context): Pair<Path, Long> {
    val file = File(context.cacheDir, "temp_path")
    var size: Long
    file.setWritable(true)
    context.contentResolver.openInputStream(this).use { inputStream ->
        file.outputStream().use {
            size = inputStream?.copyTo(it) ?: 0L
        }
    }
    return file.toOkioPath() to size
}

fun Context.getFileName(uri: Uri): String? = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
    else -> uri.path?.let(::File)?.name
}

private fun Context.getContentFileName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrNull()

fun Context.startFileShareIntent(path: String) {
    val file = File(path)
    val fileURI = FileProvider.getUriForFile(
        this, getProviderAuthority(),
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

fun saveFileToDownloadsFolder(
    assetName: String,
    assetDataPath: Path,
    assetDataSize: Long,
    context: Context,
    kaliumFileSystem: KaliumFileSystem
) {
    context.saveFileDataToDownloadsFolder(assetDataPath, assetDataSize, kaliumFileSystem)
}

fun openAssetFileWithExternalApp(
    assetName: String,
    assetDataPath: Path,
    context: Context,
    kaliumFileSystem: KaliumFileSystem,
    onError: () -> Unit
) {
    val assetUri = context.copyDataToTempShareableFile(assetName, assetDataPath, kaliumFileSystem)

    // Set intent and launch
    val intent = Intent()
    intent.setActionViewIntentFlags()
    intent.setDataAndType(assetUri, assetUri.getMimeType(context))

    try {
        context.startActivity(intent)
    } catch (noActivityFoundException: ActivityNotFoundException) {
        appLogger.e("Couldn't find a proper app to process the asset")
        onError()
    }
}

private fun Intent.setActionViewIntentFlags() {
    action = Intent.ACTION_VIEW
    // These flags allow the external app to access the temporal uri
    flags = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    } else {
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
}

fun Context.getProviderAuthority() = "${packageName}.provider"

private const val TEMP_IMG_ATTACHMENT_FILENAME = "temp_img_attachment.jpg"
private const val TEMP_VIDEO_ATTACHMENT_FILENAME = "temp_video_attachment.mp4"
private const val DATA_COPY_BUFFER_SIZE = 2048
