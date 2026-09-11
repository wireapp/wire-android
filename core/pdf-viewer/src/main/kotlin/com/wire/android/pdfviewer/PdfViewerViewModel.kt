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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.util.dispatchers.DispatcherProvider
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Opens a single PDF from either a local file ([localPath]) or a remote asset identified by
 * [assetId] and [remotePath], and renders its pages on demand.
 *
 * Arguments are passed through the assisted [Factory] instead of a navigation destination so the
 * screen can be hosted from any module.
 */
@WireAssistedViewModelBinding(PdfViewerManualViewModelFactoryGroup::class)
class PdfViewerViewModel @AssistedInject constructor(
    private val sourceResolver: PdfSourceResolver,
    private val dispatchers: DispatcherProvider,
    @Assisted val localPath: String?,
    @Assisted val assetId: String?,
    @Assisted val remotePath: String?,
    @Assisted val conversationId: String?,
    @Assisted val assetSize: Long,
    @Assisted val fileName: String?,
) : ViewModel() {

    @Suppress("LongParameterList")
    @AssistedFactory
    interface Factory {
        fun create(
            localPath: String?,
            assetId: String?,
            remotePath: String?,
            conversationId: String?,
            assetSize: Long,
            fileName: String?,
        ): PdfViewerViewModel
    }

    private val _state = MutableStateFlow<PdfViewerState>(PdfViewerState.Loading)
    val state: StateFlow<PdfViewerState> = _state.asStateFlow()

    @Volatile
    private var document: PdfDocument? = null
    private var loadJob: Job? = null

    /**
     * Releasing the document has to outlive [viewModelScope]: [PdfDocument.close] waits for an
     * in-flight render before it frees the native handle, and that wait must neither block the
     * main thread nor be cancelled halfway through.
     */
    private val releaseScope = CoroutineScope(SupervisorJob() + dispatchers.io())

    private val pageCache = PageBitmapCache(PageBitmapCache.defaultMaxBytes())

    init {
        load()
    }

    fun retry() {
        if (_state.value is PdfViewerState.Loading) return
        load(forceRefresh = true)
    }

    /**
     * Renders [pageIndex] at [widthPx] and caches the result. Returns `null` when the document is
     * not open (yet) or the page could not be rendered.
     */
    @Suppress("ReturnCount")
    suspend fun renderPage(pageIndex: Int, widthPx: Int): Bitmap? {
        if (widthPx <= 0) return null
        val current = document ?: return null
        val key = "$pageIndex@$widthPx"
        pageCache.get(key)?.let { return it }

        val rendered = withContext(dispatchers.io()) {
            runCatching { current.renderPage(pageIndex, widthPx) }.getOrNull()
        } ?: return null

        pageCache.put(key, rendered)
        return rendered
    }

    private fun load(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        closeDocument()
        _state.value = PdfViewerState.Loading
        loadJob = viewModelScope.launch {
            val file = sourceResolver.resolve(
                localPath = localPath,
                assetId = assetId,
                remotePath = remotePath,
                conversationId = conversationId,
                assetSize = assetSize,
                forceRefresh = forceRefresh,
                dispatcher = dispatchers.io(),
            ).getOrElse { cause ->
                _state.value = PdfViewerState.Failure(cause.toViewerError())
                return@launch
            }

            var opened: PdfDocument? = null
            var published = false
            try {
                val doc = withContext(dispatchers.io()) {
                    openPdfDocument(file).onSuccess { opened = it }
                }.getOrElse { cause ->
                    _state.value = PdfViewerState.Failure(cause.toViewerError())
                    return@launch
                }

                if (doc.pageCount == 0) {
                    _state.value = PdfViewerState.Failure(PdfViewerError.INVALID_DOCUMENT)
                    return@launch
                }

                val firstPageAspectRatio = withContext(dispatchers.io()) { doc.aspectRatio(0) }

                document = doc
                published = true
                _state.value = PdfViewerState.Content(
                    pageCount = doc.pageCount,
                    firstPageAspectRatio = firstPageAspectRatio,
                )
            } finally {
                // Anything not handed over to [document] is ours to release: an empty document, a
                // cancelled open, or a cancelled first-page read.
                if (!published) opened?.close()
            }
        }
    }

    private fun closeDocument() {
        pageCache.clear()
        val open = document ?: return
        document = null
        releaseScope.launch { open.close() }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
        closeDocument()
    }
}

private fun Throwable.toViewerError(): PdfViewerError = when (this) {
    is PdfSourceException -> error
    is SecurityException -> PdfViewerError.PASSWORD_PROTECTED
    is IOException -> PdfViewerError.INVALID_DOCUMENT
    else -> PdfViewerError.INVALID_DOCUMENT
}
