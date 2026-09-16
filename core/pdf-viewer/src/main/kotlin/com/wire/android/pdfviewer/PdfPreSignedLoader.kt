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
 * Downloads a PDF served by a self-contained URL, such as the pre-signed URL of a PDF rendition
 * the backend generated for a document it can convert.
 *
 * Kept separate from [PdfRemoteLoader] because such a URL identifies the file on its own: there is
 * no asset to look up and nothing to record in the attachments DB. In production it is backed by
 * `DownloadCellVersionUseCase`.
 */
fun interface PdfPreSignedLoader {
    /**
     * Downloads the PDF served by [url] into [outFile].
     */
    suspend fun load(url: String, outFile: File): Result<Unit>
}
