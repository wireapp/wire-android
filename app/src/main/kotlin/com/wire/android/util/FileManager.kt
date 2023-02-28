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
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
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

    fun openWithExternalApp(assetDataPath: Path, assetExtension: String?, onError: () -> Unit) {
        openAssetFileWithExternalApp(assetDataPath, context, assetExtension, onError)
    }

    fun shareWithExternalApp(assetDataPath: Path, assetExtension: String?, onError: () -> Unit) {
        shareAssetFileWithExternalApp(assetDataPath, context, assetExtension, onError)
    }

    suspend fun copyToTempPath(uri: Uri, tempCachePath: Path, dispatcher: DispatcherProvider = DefaultDispatcherProvider()): Long =
        withContext(dispatcher.io()) {
            val file = tempCachePath.toFile()
            var size: Long
            file.setWritable(true)
            context.contentResolver.openInputStream(uri).use { inputStream ->
                file.outputStream().use {
                    size = inputStream?.copyTo(it) ?: -1L
                }
            }
            return@withContext size
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

    companion object {
        private const val TEMP_IMG_ATTACHMENT_FILENAME = "image_attachment.jpg"
        private const val TEMP_VIDEO_ATTACHMENT_FILENAME = "video_attachment.mp4"
    }
}
