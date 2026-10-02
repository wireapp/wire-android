/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 */
package com.wire.android.feature.cells.ui.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.wire.android.di.ApplicationContext
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.CellUploadRequest
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.withContext
import okio.source
import java.io.File
import java.util.UUID

/**
 * Stages a picked [Uri] into a local file [CellUploadManager][com.wire.kalium.cells.domain.CellUploadManager]
 * can upload, since the file picker only hands back content Uris. The staged copy is read by
 * [com.wire.kalium.cells.domain.CellUploadCoordinator] for the lifetime of the upload; it is not cleaned up
 * here.
 */
class DriveUploadFilePreparer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatchers: DispatcherProvider,
) {

    /** Resolves a picked [Uri]'s display name without staging its content, for showing it before upload starts. */
    suspend fun fileName(uri: Uri): String? = withContext(dispatchers.io()) { fileNameOf(uri) }

    suspend fun prepare(uri: Uri, destinationFolderPath: String): CellUploadRequest? = withContext(dispatchers.io()) {
        val fileName = fileNameOf(uri) ?: return@withContext null
        val source = runCatching { context.contentResolver.openInputStream(uri)?.source() }.getOrNull()
            ?: return@withContext null
        val localPath = kaliumFileSystem.tempFilePath(UUID.randomUUID().toString())
        val sizeBytes = source.use { kaliumFileSystem.writeData(kaliumFileSystem.sink(localPath), it) }
        CellUploadRequest(
            localPath = localPath,
            fileName = fileName,
            sizeBytes = sizeBytes,
            destinationFolderPath = destinationFolderPath,
        )
    }

    private fun fileNameOf(uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> contentFileNameOf(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun contentFileNameOf(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
    }.getOrNull()
}
