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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.File

/**
 * Thin coroutine-friendly wrapper around the platform [PdfRenderer].
 *
 * Everything goes through [mutex] because [PdfRenderer] only allows a single open page at a time
 * and is not thread safe. Rendering happens entirely in-process — no network access and no
 * third party parser — which keeps documents from ever leaving the device.
 */
internal class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {

    private val mutex = Mutex()
    private var closed = false

    val pageCount: Int = renderer.pageCount

    /** Width / height of [pageIndex], used to reserve the right amount of space before rendering. */
    suspend fun aspectRatio(pageIndex: Int): Float = mutex.withLock {
        if (closed) return DEFAULT_ASPECT_RATIO
        renderer.openPage(pageIndex).use { page ->
            if (page.height == 0) DEFAULT_ASPECT_RATIO else page.width.toFloat() / page.height
        }
    }

    /**
     * Renders [pageIndex] into a bitmap [widthPx] wide, keeping the page aspect ratio.
     *
     * Returns `null` when the document was closed while the caller was waiting for the lock.
     */
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap? = mutex.withLock {
        if (closed) return null
        renderer.openPage(pageIndex).use { page ->
            val safeWidth = widthPx.coerceIn(MIN_RENDER_WIDTH_PX, MAX_RENDER_WIDTH_PX)
            val height = if (page.width == 0) {
                safeWidth
            } else {
                (safeWidth.toLong() * page.height / page.width).toInt()
            }.coerceIn(MIN_RENDER_WIDTH_PX, MAX_RENDER_WIDTH_PX)

            Bitmap.createBitmap(safeWidth, height, Bitmap.Config.ARGB_8888).apply {
                // PdfRenderer draws only the page content, so the paper itself has to be painted.
                eraseColor(Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        const val DEFAULT_ASPECT_RATIO = 1f / 1.414f // A4 portrait
        private const val MIN_RENDER_WIDTH_PX = 1
        private const val MAX_RENDER_WIDTH_PX = 4_096

        /**
         * Opens [file] for rendering, translating the platform failures into a [PdfViewerError].
         *
         * [PdfRenderer] throws [SecurityException] for password protected documents and
         * [java.io.IOException] for anything it cannot parse.
         */
        fun open(file: File): Result<PdfDocument> = runCatching {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                PdfDocument(descriptor, PdfRenderer(descriptor))
            } catch (error: Throwable) {
                runCatching { descriptor.close() }
                throw error
            }
        }
    }
}
