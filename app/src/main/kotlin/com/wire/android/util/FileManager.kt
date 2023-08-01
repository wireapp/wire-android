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

package com.wire.android.util

import android.content.Context
import android.net.Uri
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun saveToExternalStorage(
        assetName: String,
        assetDataPath: Path,
        assetDataSize: Long,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
        onFileSaved: suspend (String?) -> Unit
    ): Unit = withContext(dispatcher.io()) {
        saveFileToDownloadsFolder(assetName, assetDataPath, assetDataSize, context)
            ?.let { context.getFileName(it) }
            .also { onFileSaved(it) }
    }

    suspend fun saveToExternalMediaStorage(
        assetName: String,
        assetDataPath: Path,
        assetDataSize: Long,
        assetMimeType: String,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider()
    ): String? = withContext(dispatcher.io()) {
        saveFileDataToMediaFolder(assetName, assetDataPath, assetDataSize, assetMimeType, context)?.let { context.getFileName(it) }
    }

    fun openWithExternalApp(assetDataPath: Path, assetName: String?, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetDataPath, context, assetName, onError)
    }

    fun shareWithExternalApp(assetDataPath: Path, assetName: String?, onError: () -> Unit) {
        shareAssetFileWithExternalApp(assetDataPath, context, assetName, onError)
    }

    suspend fun copyToPath(uri: Uri, path: Path, dispatcher: DispatcherProvider = DefaultDispatcherProvider()): Long =
        withContext(dispatcher.io()) {
            val file = path.toFile()
            var size: Long
            file.setWritable(true)
            context.contentResolver.openInputStream(uri).use { inputStream ->
                file.outputStream().use {
                    size = inputStream?.copyTo(it) ?: -1L
                }
            }
            return@withContext size
        }

    suspend fun copyToUri(
        sourcePath: Path,
        destinationUri: Uri,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    ) =
        withContext(dispatcher.io()) {
            context.contentResolver.copyFile(destinationUri, sourcePath)
        }

    suspend fun getTempWritableVideoUri(
        tempCachePath: Path,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    ): Uri = withContext(dispatcher.io()) {
        val tempVideoPath = "$tempCachePath/$TEMP_VIDEO_ATTACHMENT_FILENAME".toPath()
        return@withContext getTempWritableAttachmentUri(context, tempVideoPath)
    }

    suspend fun getTempWritableImageUri(
        tempCachePath: Path,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    ): Uri = withContext(dispatcher.io()) {
        val tempImagePath = "$tempCachePath/$TEMP_IMG_ATTACHMENT_FILENAME".toPath()
        return@withContext getTempWritableAttachmentUri(context, tempImagePath)
    }

    suspend fun getAssetBundleFromUri(
        attachmentUri: Uri,
        tempCachePath: Path,
        audioPath: Path? = null,
        dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    ): AssetBundle? = withContext(dispatcher.io()) {
        try {
            val assetFileName = context.getFileName(attachmentUri)
                ?: throw IOException("The selected asset has an invalid name")
            val fullTempAssetPath = "$tempCachePath/${UUID.randomUUID()}".toPath()
            val assetPath = audioPath ?: fullTempAssetPath
            val mimeType = if (audioPath != null) AUDIO_MIME_TYPE else attachmentUri
                .getMimeType(context)
                .orDefault(DEFAULT_FILE_MIME_TYPE)
            val attachmentType = AttachmentType.fromMimeTypeString(mimeType)
            val assetSize = if (attachmentType == AttachmentType.IMAGE) {
                attachmentUri.resampleImageAndCopyToTempPath(context, fullTempAssetPath)
            } else {
                // TODO: We should add also a video resampling logic soon, that way we could drastically reduce as well the number
                //  of video assets hitting the max limit.
                copyToPath(attachmentUri, fullTempAssetPath)
            }
            AssetBundle(mimeType, assetPath, assetSize, assetFileName, attachmentType)
        } catch (e: IOException) {
            appLogger.e("There was an error while obtaining the file from disk", e)
            null
        }
    }

    companion object {
        private const val TEMP_IMG_ATTACHMENT_FILENAME = "image_attachment.jpg"
        private const val TEMP_VIDEO_ATTACHMENT_FILENAME = "video_attachment.mp4"
    }
}
