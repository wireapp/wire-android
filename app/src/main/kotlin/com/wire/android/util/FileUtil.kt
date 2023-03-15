/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

@file:Suppress("TooManyFunctions")

package com.wire.android.util

import com.wire.android.R
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.annotation.AnyRes
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.wire.android.appLogger
import com.wire.android.util.ImageUtil.ImageSizeClass
import com.wire.android.util.ImageUtil.ImageSizeClass.Medium
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.util.buildFileName
import com.wire.kalium.logic.util.splitFileExtensionAndCopyCounter
import kotlinx.coroutines.withContext
import okio.Path
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Locale

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
suspend fun Uri.toByteArray(context: Context, dispatcher: DispatcherProvider = DefaultDispatcherProvider()): ByteArray {
    return withContext(dispatcher.io()) {
        context.contentResolver.openInputStream(this@toByteArray)?.use { it.readBytes() } ?: ByteArray(16)
    }
}

suspend fun Uri.toDrawable(context: Context, dispatcher: DispatcherProvider = DefaultDispatcherProvider()): Drawable? {
    val dataUri = this
    return withContext(dispatcher.io()) {
        try {
            context.contentResolver.openInputStream(dataUri).use { inputStream ->
                Drawable.createFromStream(inputStream, dataUri.toString())
            }
        } catch (e: FileNotFoundException) {
            defaultGalleryIcon(context)
        }
    }
}

private fun defaultGalleryIcon(context: Context) = ContextCompat.getDrawable(context, R.drawable.ic_gallery)

fun getTempWritableAttachmentUri(context: Context, attachmentPath: Path): Uri {
    val file = attachmentPath.toFile()
    file.setWritable(true)
    return FileProvider.getUriForFile(context, context.getProviderAuthority(), file)
}

private fun Context.saveFileDataToDownloadsFolder(assetName: String, downloadedDataPath: Path, fileSize: Long): Uri? {
    val resolver = contentResolver
    val mimeType = Uri.parse(downloadedDataPath.toString()).getMimeType(this@saveFileDataToDownloadsFolder)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            // ContentResolver modifies the name if another file with the given name already exists, so we don't have to worry about it
            put(MediaStore.MediaColumns.DISPLAY_NAME, assetName.ifEmpty { ATTACHMENT_FILENAME })
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.SIZE, fileSize)
        }
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val authority = getProviderAuthority()
        val downloadsDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        // we need to find the next available name with copy counter by ourselves before copying
        val availableAssetName = findFirstUniqueName(downloadsDir, assetName.ifEmpty { ATTACHMENT_FILENAME })
        val destinationFile = File(downloadsDir, availableAssetName)
        val uri = FileProvider.getUriForFile(this, authority, destinationFile)
        if (mimeType?.isNotEmpty() == true) {
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.addCompletedDownload(
                /* title = */ availableAssetName,
                /* description = */ availableAssetName,
                /* isMediaScannerScannable = */ true,
                /* mimeType = */ mimeType,
                /* path = */ destinationFile.absolutePath,
                /* length = */ fileSize,
                /* showNotification = */ false
            )
        }
        uri
    }?.also { downloadedUri ->
        resolver.openOutputStream(downloadedUri).use { outputStream ->
            val brr = ByteArray(DATA_COPY_BUFFER_SIZE)
            var len: Int
            val bufferedInputStream: InputStream = File(downloadedDataPath.toString()).inputStream()
            while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                outputStream?.write(brr, 0, len)
            }
            outputStream?.flush()
            bufferedInputStream.close()
        }
    }
}

fun Context.pathToUri(assetDataPath: Path): Uri = FileProvider.getUriForFile(this, getProviderAuthority(), assetDataPath.toFile())

fun Uri.getMimeType(context: Context): String? {
    val extension: String? = if (this.scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.getType(this)
    } else {
        // If scheme is a File
        // This will replace white spaces with %20 and also other special characters.
        // This will avoid returning null values on file name with spaces and special characters.
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(this.path?.let { File(it) }).toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
    }
    return extension
}

suspend fun Uri.resampleImageAndCopyToTempPath(
    context: Context,
    tempCachePath: Path,
    sizeClass: ImageSizeClass = Medium,
    dispatcher: DispatcherProvider = DefaultDispatcherProvider()
): Long {
    return withContext(dispatcher.io()) {
        var size: Long
        val originalImage = toByteArray(context, dispatcher)
        if (originalImage.isEmpty()) return@withContext 0L // if the image is empty, resampling it would cause an exception

        ImageUtil.resample(originalImage, sizeClass).let { processedImage ->
            val file = tempCachePath.toFile()
            size = processedImage.size.toLong()
            file.setWritable(true)
            file.outputStream().use { it.write(processedImage) }
        }

        size
    }
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
        resources.getString(R.string.export_media_subject_title)
    )

    shareIntent.putExtra(Intent.EXTRA_STREAM, fileURI)
    shareIntent.type = fileURI.getMimeType(context = this)
    startActivity(shareIntent)
}

fun saveFileToDownloadsFolder(assetName: String, assetDataPath: Path, assetDataSize: Long, context: Context): Uri? =
    context.saveFileDataToDownloadsFolder(assetName, assetDataPath, assetDataSize)

fun Context.multipleFileSharingIntent(uris: ArrayList<Uri>): Intent {

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    return intent
}

fun Context.getUrisOfFilesInDirectory(dir: File): ArrayList<Uri> {

    val files = ArrayList<Uri>()

    dir.listFiles()?.map {
        val uri = FileProvider.getUriForFile(
            this, getProviderAuthority(),
            it
        )
        files.add(uri)
    }

    return files
}

fun openAssetFileWithExternalApp(assetDataPath: Path, context: Context, assetExtension: String?, onError: () -> Unit) {
    try {
        val assetUri = context.pathToUri(assetDataPath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(assetExtension)
        // Set intent and launch
        val intent = Intent()
        intent.apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(assetUri, mimeType)
        }
        context.startActivity(intent)
    } catch (e: java.lang.IllegalArgumentException) {
        appLogger.e("The file couldn't be found on the internal storage \n$e")
        onError()
    } catch (noActivityFoundException: ActivityNotFoundException) {
        appLogger.e("Couldn't find a proper app to process the asset")
        onError()
    }
}

fun shareAssetFileWithExternalApp(assetDataPath: Path, context: Context, assetExtension: String?, onError: () -> Unit) {
    try {
        val assetUri = context.pathToUri(assetDataPath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(assetExtension)
        // Set intent and launch
        val intent = Intent()
        intent.apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(assetUri, mimeType)
            putExtra(Intent.EXTRA_STREAM, assetUri)
        }
        context.startActivity(intent)
    } catch (e: java.lang.IllegalArgumentException) {
        appLogger.e("The file couldn't be found on the internal storage \n$e")
        onError()
    } catch (noActivityFoundException: ActivityNotFoundException) {
        appLogger.e("Couldn't find a proper app to process the asset")
        onError()
    }
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= SDK_VERSION -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= SDK_VERSION -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

fun Uri.getMetaDataFromUri(context: Context): FileMetaData {
    context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            val fileMimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
            return FileMetaData(displayName, size, fileMimeType)
        }
    }
    return FileMetaData()
}

data class FileMetaData(val name: String = "", val size: Long = 0L, val mimeType: String = "")

fun isImageFile(mimeType: String?): Boolean {
    return mimeType != null && mimeType.startsWith("image/")
}

fun isVideoFile(mimeType: String?): Boolean {
    return mimeType != null && mimeType.startsWith("video/")
}

fun isAudioFile(mimeType: String?): Boolean {
    return mimeType != null && mimeType.startsWith("audio/")
}

fun isText(mimeType: String?): Boolean {
    return mimeType != null && mimeType.startsWith("text/")
}

@Suppress("MagicNumber")
fun Context.getDeviceId(): String? {

    if (Build.VERSION.SDK_INT >= 26) {
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    return null
}

fun Context.getGitBuildId(): String = runCatching {
    assets.open("version.txt").use { inputStream ->
        inputStream.bufferedReader().use { it.readText() }
    }
}.getOrDefault("")

fun Context.getProviderAuthority() = "$packageName.provider"

@VisibleForTesting
fun findFirstUniqueName(dir: File, desiredName: String): String {
    var currentName: String = desiredName
    while (File(dir, currentName).exists()) {
        val (nameWithoutCopyCounter, copyCounter, extension) = currentName.splitFileExtensionAndCopyCounter()
        currentName = buildFileName(nameWithoutCopyCounter, extension, copyCounter + 1)
    }
    return currentName
}

private const val ATTACHMENT_FILENAME = "attachment"
private const val TEMP_IMG_ATTACHMENT_FILENAME = "image_attachment.jpg"
private const val DATA_COPY_BUFFER_SIZE = 2048
const val SDK_VERSION = 33
