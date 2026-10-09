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
import java.io.Closeable
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.sqrt

/**
 * Thin wrapper around the platform [PdfRenderer]. Obtain instances via [openPdfDocument].
 *
 * Everything goes through [lock] because [PdfRenderer] only allows a single open page at a time
 * and is not thread safe. Rendering happens entirely in-process — no network access and no
 * third party parser — which keeps documents from ever leaving the device.
 */
internal class PdfDocument internal constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {

    /**
     * Guards every access to [renderer], including [close].
     *
     * A blocking lock rather than a coroutine `Mutex` on purpose: [close] is not a suspending
     * function, so it could never have joined a `Mutex`, and releasing the native handle while a
     * render is in flight crashes inside PdfRenderer — below the level any `runCatching` could
     * recover from. Callers must therefore be off the main thread; [PdfViewerViewModel] dispatches
     * all of them to IO.
     */
    private val lock = ReentrantLock()
    private var closed = false

    val pageCount: Int = renderer.pageCount

    /**
     * Width / height of [pageIndex], used to reserve the right amount of space before rendering.
     *
     * Never throws: `openPage` can fail on a malformed page, and callers reserve space from
     * non-failing contexts, so a broken page falls back to the default ratio instead.
     */
    fun aspectRatio(pageIndex: Int): Float = lock.withLock {
        if (closed) return DEFAULT_ASPECT_RATIO
        runCatching {
            renderer.openPage(pageIndex).use { page ->
                if (page.height == 0) DEFAULT_ASPECT_RATIO else page.width.toFloat() / page.height
            }
        }.getOrDefault(DEFAULT_ASPECT_RATIO)
    }

    /**
     * Renders [pageIndex] into a bitmap [widthPx] wide, keeping the page aspect ratio.
     *
     * Returns `null` when the document was closed while the caller was waiting for the lock.
     */
    fun renderPage(pageIndex: Int, widthPx: Int): Bitmap? = lock.withLock {
        if (closed) return null
        renderer.openPage(pageIndex).use { page ->
            val (width, height) = renderSize(page.width, page.height, widthPx)

            // Must be ARGB_8888: PdfRenderer.Page.render rejects every other config except
            // ALPHA_8 with "Unsupported pixel format". Memory is bounded by MAX_RENDER_PIXELS.
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                // PdfRenderer draws only the page content, so the paper itself has to be painted.
                eraseColor(Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    /**
     * Releases the renderer and the file descriptor. Blocks until any in-flight page has finished
     * rendering, so it must not be called from the main thread.
     */
    override fun close() {
        lock.withLock {
            if (closed) return
            closed = true
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        }
    }

    companion object {
        /** A4 portrait: the shape assumed for a page that could not be measured. */
        const val DEFAULT_ASPECT_RATIO = 1f / 1.414f

        const val MIN_RENDER_PX = 1

        /**
         * Total pixel budget for one page bitmap: 16 MB at [Bitmap.Config.ARGB_8888]
         *
         * Bounding the area rather than the width is what keeps tall pages undistorted: a width
         * cap alone would squash an A4 page once the requested width pushed its height past the
         * limit.
         */
        const val MAX_RENDER_PIXELS = 4_000_000L
    }
}

/**
 * Opens [file] for rendering, translating the platform failures into a [PdfViewerError].
 *
 * [PdfRenderer] throws [SecurityException] for password protected documents and
 * [java.io.IOException] for anything it cannot parse.
 */
@Suppress("TooGenericExceptionCaught")
internal fun openPdfDocument(file: File): Result<PdfDocument> = runCatching {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        PdfDocument(descriptor, PdfRenderer(descriptor))
    } catch (error: Throwable) {
        runCatching { descriptor.close() }
        throw error
    }
}

/**
 * Size of the bitmap for a [pageWidth] x [pageHeight] page requested at [requestedWidth],
 * preserving the page aspect ratio and never exceeding [PdfDocument.MAX_RENDER_PIXELS].
 */
internal fun renderSize(pageWidth: Int, pageHeight: Int, requestedWidth: Int): Pair<Int, Int> {
    val ratio = if (pageWidth <= 0 || pageHeight <= 0) {
        PdfDocument.DEFAULT_ASPECT_RATIO
    } else {
        pageWidth.toFloat() / pageHeight
    }

    val width = requestedWidth.coerceAtLeast(PdfDocument.MIN_RENDER_PX)
    val height = (width / ratio).toInt().coerceAtLeast(PdfDocument.MIN_RENDER_PX)

    val pixels = width.toLong() * height
    if (pixels <= PdfDocument.MAX_RENDER_PIXELS) return width to height

    // Shrink both axes by the same factor so the page keeps its shape.
    val factor = sqrt(PdfDocument.MAX_RENDER_PIXELS.toDouble() / pixels)
    return (width * factor).toInt().coerceAtLeast(PdfDocument.MIN_RENDER_PX) to
            (height * factor).toInt().coerceAtLeast(PdfDocument.MIN_RENDER_PX)
}
