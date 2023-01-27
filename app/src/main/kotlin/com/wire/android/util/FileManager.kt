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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun saveToExternalStorage(
        assetName: String,
        assetDataPath: Path,
        assetDataSize: Long,
        onFileSaved: suspend (String?) -> Unit
    ) {
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
}
