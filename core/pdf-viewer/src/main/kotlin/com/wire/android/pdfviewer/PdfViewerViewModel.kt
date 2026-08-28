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
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Opens a single PDF from either a local file ([localPath]) or a remote [contentUrl] and renders
 * its pages on demand.
 *
 * Arguments are passed through the assisted [Factory] instead of a navigation destination so the
 * screen can be hosted from any module.
 */
@WireAssistedViewModelBinding(PdfViewerManualViewModelFactoryGroup::class)
class PdfViewerViewModel @AssistedInject constructor(
    private val sourceResolver: PdfSourceResolver,
    @Assisted val localPath: String?,
    @Assisted val contentUrl: String?,
    @Assisted val fileName: String?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(localPath: String?, contentUrl: String?, fileName: String?): PdfViewerViewModel
    }

    private val _state = MutableStateFlow<PdfViewerState>(PdfViewerState.Loading)
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    private var document: PdfDocument? = null
    private var loadJob: Job? = null

    /**
     * Keeps recently rendered pages around so scrolling back does not re-rasterise them.
     * Sized against the heap rather than a page count because page bitmaps vary a lot in size.
     */
    private val pageCache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / BYTES_IN_KB
    }

    init {
        load()
    }

    fun retry() {
        if (_state.value is PdfViewerState.Loading) return
        load()
    }

    /**
     * Renders [pageIndex] at [widthPx] and caches the result. Returns `null` when the document is
     * not open (yet) or the page could not be rendered.
     */
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap? {
        if (widthPx <= 0) return null
        val current = document ?: return null
        val key = "$pageIndex@$widthPx"
        pageCache.get(key)?.let { return it }

        val rendered = withContext(renderDispatcher) {
            runCatching { current.renderPage(pageIndex, widthPx) }.getOrNull()
        } ?: return null

        pageCache.put(key, rendered)
        return rendered
    }

    private fun load() {
        loadJob?.cancel()
        closeDocument()
        _state.value = PdfViewerState.Loading
        loadJob = viewModelScope.launch {
            val file = sourceResolver.resolve(localPath, contentUrl, renderDispatcher)
                .getOrElse { cause ->
                    _state.value = PdfViewerState.Failure(cause.toViewerError())
                    return@launch
                }

            val opened = withContext(renderDispatcher) { PdfDocument.open(file) }
                .getOrElse { cause ->
                    _state.value = PdfViewerState.Failure(cause.toViewerError())
                    return@launch
                }

            if (opened.pageCount == 0) {
                opened.close()
                _state.value = PdfViewerState.Failure(PdfViewerError.INVALID_DOCUMENT)
                return@launch
            }

            document = opened
            _state.value = PdfViewerState.Content(
                pageCount = opened.pageCount,
                firstPageAspectRatio = withContext(renderDispatcher) { opened.aspectRatio(0) },
            )
        }
    }

    private fun closeDocument() {
        pageCache.evictAll()
        document?.close()
        document = null
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        closeDocument()
    }

    private companion object {
        val renderDispatcher: CoroutineDispatcher = Dispatchers.IO
        const val BYTES_IN_KB = 1024
        const val CACHE_HEAP_FRACTION = 8

        fun cacheSizeKb(): Int =
            (Runtime.getRuntime().maxMemory() / BYTES_IN_KB / CACHE_HEAP_FRACTION)
                .coerceAtLeast(1)
                .toInt()
    }
}

private fun Throwable.toViewerError(): PdfViewerError = when {
    this is PdfSourceException -> error
    this is SecurityException -> PdfViewerError.PASSWORD_PROTECTED
    this is IOException -> PdfViewerError.INVALID_DOCUMENT
    else -> PdfViewerError.INVALID_DOCUMENT
}
