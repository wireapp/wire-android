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
package com.wire.android.pdfviewer

import android.content.Context
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Turns the arguments of the PDF screen into a readable local file.
 *
 * [android.graphics.pdf.PdfRenderer] needs a seekable file descriptor, so a remote asset has
 * to be fetched into the app cache first. Already downloaded files are reused, which keeps
 * re-opening the same attachment instant.
 *
 * Remote downloads are delegated to [PdfRemoteLoader], which is backed in production by
 * `DownloadCellFileUseCase` — the same authenticated kalium S3 client used for offline file
 * downloads. This ensures authentication, retry logic, and download progress tracking are
 * handled consistently with the rest of the app.
 */
class PdfSourceResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteLoader: PdfRemoteLoader,
) {

    suspend fun resolve(
        localPath: String?,
        assetId: String?,
        remotePath: String?,
        conversationId: String?,
        assetSize: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Result<File> = withContext(dispatcher) {
        val localFile = localPath?.let(::File)
        when {
            localFile != null && localFile.isReadableFile() -> Result.success(localFile)
            assetId != null && remotePath != null ->
                download(assetId, remotePath, conversationId, assetSize)
            else -> Result.failure(PdfSourceException(PdfViewerError.FILE_NOT_FOUND))
        }
    }

    private suspend fun download(
        assetId: String,
        remotePath: String,
        conversationId: String?,
        assetSize: Long,
    ): Result<File> {
        val target = cacheFileFor(assetId)
        if (target.isReadableFile()) return Result.success(target)

        val partial = File(target.parentFile, "${target.name}$PARTIAL_SUFFIX")
        return runCatching {
            partial.parentFile?.mkdirs()
            remoteLoader.load(assetId, remotePath, conversationId, assetSize, partial).getOrThrow()
            check(partial.renameTo(target)) { "Could not move the downloaded document into place" }
            target
        }.recoverCatching { cause ->
            partial.delete()
            throw PdfSourceException(PdfViewerError.DOWNLOAD_FAILED, cause)
        }
    }

    private fun cacheFileFor(assetId: String): File =
        File(File(context.cacheDir, CACHE_DIR_NAME), "$assetId.pdf")

    private fun File.isReadableFile(): Boolean = isFile && canRead() && length() > 0

    private companion object {
        const val CACHE_DIR_NAME = "pdf-viewer"
        const val PARTIAL_SUFFIX = ".part"
    }
}

/** Carries the user-facing [error] out of [PdfSourceResolver]. */
class PdfSourceException(
    val error: PdfViewerError,
    cause: Throwable? = null,
) : Exception(cause)
