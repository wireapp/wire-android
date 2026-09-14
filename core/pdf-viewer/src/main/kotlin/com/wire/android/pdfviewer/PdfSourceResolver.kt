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
 * to be fetched to local storage first. Already downloaded files are reused, which keeps
 * re-opening the same attachment instant.
 *
 * Downloads land in the same place as every other cells download — the per-conversation folder
 * under app-specific external storage — rather than in a cache directory of our own, because
 * `DownloadCellFileUseCase` records the path it is given in the attachments DB. A cache path
 * would let the OS delete the file while the DB still reported it as downloaded, and a private
 * directory would leave a second copy of a file the rest of the app already knows how to find.
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

    /**
     * @param forceRefresh re-downloads even when a local copy exists, which is how a document
     *   that turned out to be unopenable gets a second chance.
     */
    suspend fun resolve(
        source: PdfDocumentSource,
        forceRefresh: Boolean = false,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Result<File> = withContext(dispatcher) {
        val localFile = source.localPath?.let(::File)
        val assetId = source.assetId
        when {
            localFile != null && localFile.isReadableFile() -> Result.success(localFile)
            assetId != null -> download(source, assetId, forceRefresh)
            else -> Result.failure(PdfSourceException(PdfViewerError.FILE_NOT_FOUND))
        }
    }

    private suspend fun download(
        source: PdfDocumentSource,
        assetId: String,
        forceRefresh: Boolean,
    ): Result<File> {
        val target = downloadFileFor(assetId, source.conversationId, source.remotePath, source.fileName)
            ?: return Result.failure(PdfSourceException(PdfViewerError.FILE_NOT_FOUND))
        if (!forceRefresh && target.isReadableFile()) return Result.success(target)

        return remoteLoader.load(assetId, source.remotePath, source.conversationId, source.assetSize, target).fold(
            onSuccess = { Result.success(target) },
            onFailure = { Result.failure(PdfSourceException(PdfViewerError.DOWNLOAD_FAILED, it)) },
        )
    }

    /**
     * `<externalFilesDir>/<conversationId>/<sub>/<folders>/<name>`, creating the folders as needed.
     *
     * [remotePath] is the cells object key and is already rooted at the conversation, so mirroring
     * it reproduces the remote structure locally and keeps two same-named files in different
     * folders apart.
     *
     * Returns null when [remotePath] would escape the download directory: it comes from the
     * backend, so it is not trusted as a path.
     */
    private fun downloadFileFor(
        assetId: String,
        conversationId: String?,
        remotePath: String?,
        fileName: String?,
    ): File? {
        val root = externalFilesDir()
        val fallbackFolder = conversationId ?: assetId

        val relative = remotePath.orEmpty().trim('/').let { trimmed ->
            when {
                trimmed.isEmpty() -> "$fallbackFolder/${fileName ?: "$assetId.pdf"}"
                !trimmed.contains('/') -> "$fallbackFolder/$trimmed"
                else -> trimmed
            }
        }

        val target = File(root, relative)
        val insideRoot = runCatching {
            target.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
        }.getOrDefault(false)
        if (!insideRoot) return null

        target.parentFile?.mkdirs()
        return target
    }

    private fun externalFilesDir(): File = context.getExternalFilesDir(null) ?: context.filesDir

    private fun File.isReadableFile(): Boolean = isFile && canRead() && length() > 0
}

/** Carries the user-facing [error] out of [PdfSourceResolver]. */
class PdfSourceException(
    val error: PdfViewerError,
    cause: Throwable? = null,
) : Exception(cause)
