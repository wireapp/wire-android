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

/** Everything the PDF screen needs to draw itself. */
sealed interface PdfViewerState {

    /** The document is being fetched and/or parsed. */
    data object Loading : PdfViewerState

    /** The document is ready; [pageCount] pages can be requested from the ViewModel. */
    data class Content(
        val pageCount: Int,
        val firstPageAspectRatio: Float,
    ) : PdfViewerState

    /** The document could not be shown. */
    data class Failure(val error: PdfViewerError) : PdfViewerState
}

enum class PdfViewerError {
    /** No local path and no content URL were given, or the local file is gone. */
    FILE_NOT_FOUND,

    /** The content URL could not be fetched. */
    DOWNLOAD_FAILED,

    /** The document is encrypted and needs a password, which is not supported. */
    PASSWORD_PROTECTED,

    /** The bytes are not a PDF we can parse. */
    INVALID_DOCUMENT,
}
