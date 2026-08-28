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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class PdfSourceResolverTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun givenAReadableLocalFile_whenResolving_thenThatFileIsReturnedWithoutDownloading() = runTest {
        val document = File(tempDir, "document.pdf").apply { writeText("%PDF-1.4") }
        val resolver = resolver()

        val result = resolver.resolve(document.absolutePath, contentUrl = null, dispatcher = Dispatchers.Default)

        assertEquals(document, result.getOrNull())
    }

    @Test
    fun givenNoLocalFileAndNoUrl_whenResolving_thenItFailsAsNotFound() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(localPath = null, contentUrl = null, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAnEmptyLocalFileAndNoUrl_whenResolving_thenItFailsAsNotFound() = runTest {
        val empty = File(tempDir, "empty.pdf").apply { createNewFile() }
        val resolver = resolver()

        val result = resolver.resolve(empty.absolutePath, contentUrl = null, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAnUnusableUrl_whenResolving_thenItFailsAsDownloadFailed() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(localPath = null, contentUrl = "not a url", dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    @Test
    fun givenAMissingLocalPathAndAUrl_whenResolving_thenTheDownloadPathIsUsed() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(
            localPath = File(tempDir, "gone.pdf").absolutePath,
            contentUrl = "not a url",
            dispatcher = Dispatchers.Default,
        )

        assertTrue(result.isFailure)
        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    private fun resolver(): PdfSourceResolver {
        val context = mockk<Context>()
        every { context.cacheDir } returns File(tempDir, "cache")
        return PdfSourceResolver(context)
    }

    private fun Result<File>.viewerError(): PdfViewerError? =
        (exceptionOrNull() as? PdfSourceException)?.error
}
