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

import java.io.File

/**
 * Downloads a remote PDF asset to a local [File].
 *
 * Implementations are provided by the host module so that the pdf-viewer module
 * stays decoupled from any specific networking or authentication stack.
 * In production this is backed by [DownloadCellFileUseCase] which goes through
 * the authenticated kalium S3 client.
 */
fun interface PdfRemoteLoader {
    /**
     * Downloads the asset identified by [assetId] / [remotePath] into [outFile].
     *
     * @param assetId   UUID of the cell asset.
     * @param remotePath S3 object key / remote path of the asset.
     * @param conversationId Optional conversation the asset belongs to (used for DB metadata).
     * @param assetSize Expected byte size of the asset (used for progress tracking).
     * @param outFile   Target file to write the downloaded bytes into.
     */
    suspend fun load(
        assetId: String,
        remotePath: String,
        conversationId: String?,
        assetSize: Long,
        outFile: File,
    ): Result<Unit>
}
