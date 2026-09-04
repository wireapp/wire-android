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
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.io.IOException

@ExtendWith(CoroutineTestExtension::class)
internal class PdfViewerViewModelTest {

    @AfterEach
    fun tearDown() {
        unmockkObject(PdfDocument.Companion)
    }

    @Test
    fun `given the document opens, when the view model is created, then the state exposes the page count`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPageCount(7)
            .withFirstPageAspectRatio(0.5f)
            .arrange()

        assertEquals(PdfViewerState.Content(pageCount = 7, firstPageAspectRatio = 0.5f), viewModel.state.value)
    }

    @Test
    fun `given the source cannot be resolved, when the view model is created, then that error is exposed`() = runTest {
        val (_, viewModel) = Arrangement()
            .withResolveFailure(PdfViewerError.FILE_NOT_FOUND)
            .arrange()

        assertEquals(PdfViewerState.Failure(PdfViewerError.FILE_NOT_FOUND), viewModel.state.value)
    }

    @Test
    fun `given the download fails, when the view model is created, then the failure is reported as a download error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withResolveFailure(PdfViewerError.DOWNLOAD_FAILED)
            .arrange()

        assertEquals(PdfViewerState.Failure(PdfViewerError.DOWNLOAD_FAILED), viewModel.state.value)
        verify(exactly = 0) { PdfDocument.open(any()) }
        coVerify(exactly = 1) { arrangement.sourceResolver.resolve(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given the document is password protected, when opening it, then the state reports it as protected`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOpenFailure(SecurityException("password required"))
            .arrange()

        assertEquals(PdfViewerState.Failure(PdfViewerError.PASSWORD_PROTECTED), viewModel.state.value)
    }

    @Test
    fun `given the bytes cannot be parsed, when opening the document, then the state reports an invalid document`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOpenFailure(IOException("not a pdf"))
            .arrange()

        assertEquals(PdfViewerState.Failure(PdfViewerError.INVALID_DOCUMENT), viewModel.state.value)
    }

    @Test
    fun `given a document without pages, when opening it, then it is closed and reported as invalid`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPageCount(0)
            .arrange()

        assertEquals(PdfViewerState.Failure(PdfViewerError.INVALID_DOCUMENT), viewModel.state.value)
        verify(exactly = 1) { arrangement.document.close() }
    }

    @Test
    fun `given an open document, when rendering the same page twice, then the second call comes from the cache`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        val first = viewModel.renderPage(pageIndex = 0, widthPx = 100)
        val second = viewModel.renderPage(pageIndex = 0, widthPx = 100)

        assertSame(arrangement.bitmap, first)
        assertSame(first, second)
        verify(exactly = 1) { arrangement.document.renderPage(0, 100) }
    }

    @Test
    fun `given an open document, when the requested width changes, then the page is rendered again`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.renderPage(pageIndex = 0, widthPx = 100)
        viewModel.renderPage(pageIndex = 0, widthPx = 200)

        verify(exactly = 1) { arrangement.document.renderPage(0, 100) }
        verify(exactly = 1) { arrangement.document.renderPage(0, 200) }
    }

    @Test
    fun `given a non positive width, when rendering, then nothing is rendered`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 0))
        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = -10))

        verify(exactly = 0) { arrangement.document.renderPage(any(), any()) }
    }

    @Test
    fun `given the document failed to open, when rendering, then no bitmap is returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withOpenFailure(IOException("not a pdf"))
            .arrange()

        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 100))
    }

    @Test
    fun `given rendering a page fails, when rendering it again, then nothing was cached`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRenderedPage(null)
            .arrange()

        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 100))
        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 100))

        verify(exactly = 2) { arrangement.document.renderPage(0, 100) }
    }

    @Test
    fun `given rendering throws, when rendering, then the failure is swallowed and no bitmap is returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withRenderFailure(OutOfMemoryError("bitmap too large"))
            .arrange()

        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 100))
    }

    @Test
    fun `given a loaded document, when reloading, then the previous one is closed and a new one is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.retry()

        verify(exactly = 1) { arrangement.document.close() }
        verify(exactly = 2) { PdfDocument.open(any()) }
    }

    @Test
    fun `given the document was closed by a reload that then failed, when rendering, then nothing is returned`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        arrangement.withOpenFailure(IOException("gone"))
        viewModel.retry()

        // The old document is detached before it is closed, so no render can reach a closed renderer.
        assertNull(viewModel.renderPage(pageIndex = 0, widthPx = 100))
        verify(exactly = 0) { arrangement.document.renderPage(any(), any()) }
    }

    @Test
    fun `given a failed load, when retrying, then the document is opened again`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withResolveFailure(PdfViewerError.DOWNLOAD_FAILED)
            .arrange()

        arrangement.withResolveSuccess().withPageCount(3)
        viewModel.retry()

        assertEquals(PdfViewerState.Content(pageCount = 3, firstPageAspectRatio = DEFAULT_ASPECT_RATIO), viewModel.state.value)
        coVerify(exactly = 2) { arrangement.sourceResolver.resolve(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given a load already in flight, when retrying, then the second load is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val (arrangement, viewModel) = Arrangement()
            .withResolveGatedBy(gate)
            .arrange()

        assertEquals(PdfViewerState.Loading, viewModel.state.value)
        viewModel.retry()
        coVerify(exactly = 1) { arrangement.sourceResolver.resolve(any(), any(), any(), any(), any(), any()) }

        gate.complete(Unit)

        assertEquals(
            PdfViewerState.Content(pageCount = DEFAULT_PAGE_COUNT, firstPageAspectRatio = DEFAULT_ASPECT_RATIO),
            viewModel.state.value,
        )
        coVerify(exactly = 1) { arrangement.sourceResolver.resolve(any(), any(), any(), any(), any(), any()) }
    }

    private class Arrangement {

        val sourceResolver: PdfSourceResolver = mockk()
        val document: PdfDocument = mockk(relaxed = true)
        val bitmap: Bitmap = mockk<Bitmap>(relaxed = true).also { every { it.byteCount } returns BITMAP_BYTES }

        private val file = File("document.pdf")

        init {
            mockkObject(PdfDocument.Companion)
            every { PdfDocument.open(any()) } returns Result.success(document)
            every { document.pageCount } returns DEFAULT_PAGE_COUNT
            every { document.aspectRatio(any()) } returns DEFAULT_ASPECT_RATIO
            every { document.renderPage(any(), any()) } returns bitmap
            withResolveSuccess()
        }

        fun withResolveSuccess() = apply {
            coEvery { sourceResolver.resolve(any(), any(), any(), any(), any(), any()) } returns Result.success(file)
        }

        fun withResolveFailure(error: PdfViewerError) = apply {
            coEvery { sourceResolver.resolve(any(), any(), any(), any(), any(), any()) } returns
                    Result.failure(PdfSourceException(error))
        }

        fun withResolveGatedBy(gate: CompletableDeferred<Unit>) = apply {
            coEvery { sourceResolver.resolve(any(), any(), any(), any(), any(), any()) } coAnswers {
                gate.await()
                Result.success(file)
            }
        }

        fun withOpenFailure(cause: Throwable) = apply {
            every { PdfDocument.open(any()) } returns Result.failure(cause)
        }

        fun withPageCount(count: Int) = apply {
            every { document.pageCount } returns count
        }

        fun withFirstPageAspectRatio(ratio: Float) = apply {
            every { document.aspectRatio(0) } returns ratio
        }

        fun withRenderedPage(bitmap: Bitmap?) = apply {
            every { document.renderPage(any(), any()) } returns bitmap
        }

        fun withRenderFailure(cause: Throwable) = apply {
            every { document.renderPage(any(), any()) } throws cause
        }

        fun arrange(): Pair<Arrangement, PdfViewerViewModel> = this to PdfViewerViewModel(
            sourceResolver = sourceResolver,
            dispatchers = TestDispatcherProvider(),
            localPath = "local/document.pdf",
            assetId = null,
            remotePath = null,
            conversationId = null,
            assetSize = 0L,
            fileName = "document.pdf",
        )
    }

    private companion object {
        const val DEFAULT_PAGE_COUNT = 3
        const val DEFAULT_ASPECT_RATIO = 0.7f
        const val BITMAP_BYTES = 1024
    }
}
