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
import io.mockk.coEvery
import io.mockk.coVerify
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
        val loader = mockk<PdfRemoteLoader>(relaxed = true)
        val resolver = resolver(loader)

        val result = resolver.resolve(document.absolutePath, assetId = null, remotePath = null, conversationId = null, assetSize = 0L, dispatcher = Dispatchers.Default)

        assertEquals(document, result.getOrNull())
        coVerify(exactly = 0) { loader.load(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenNoLocalFileAndNoAssetInfo_whenResolving_thenItFailsAsNotFound() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(localPath = null, assetId = null, remotePath = null, conversationId = null, assetSize = 0L, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAnEmptyLocalFileAndNoAssetInfo_whenResolving_thenItFailsAsNotFound() = runTest {
        val empty = File(tempDir, "empty.pdf").apply { createNewFile() }
        val resolver = resolver()

        val result = resolver.resolve(empty.absolutePath, assetId = null, remotePath = null, conversationId = null, assetSize = 0L, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAssetIdAndRemotePath_whenLoaderFails_thenItFailsAsDownloadFailed() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } returns Result.failure(Exception("network error"))
        }
        val resolver = resolver(loader)

        val result = resolver.resolve(localPath = null, assetId = "asset-123", remotePath = "/cells/path/doc.pdf", conversationId = null, assetSize = 1024L, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    @Test
    fun givenAssetIdButNoRemotePath_whenResolving_thenItFailsAsNotFound() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(localPath = null, assetId = "asset-123", remotePath = null, conversationId = null, assetSize = 0L, dispatcher = Dispatchers.Default)

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAMissingLocalPathAndValidAssetInfo_whenResolving_thenTheDownloadPathIsUsed() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } returns Result.failure(Exception("network error"))
        }
        val resolver = resolver(loader)

        val result = resolver.resolve(
            localPath = File(tempDir, "gone.pdf").absolutePath,
            assetId = "asset-123",
            remotePath = "/cells/path/doc.pdf",
            conversationId = null,
            assetSize = 0L,
            dispatcher = Dispatchers.Default,
        )

        assertTrue(result.isFailure)
        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    @Test
    fun givenSuccessfulDownload_whenCachedFileExists_thenLoaderIsNotCalledAgain() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } coAnswers {
                val outFile = arg<File>(4)
                outFile.writeText("%PDF-1.4")
                Result.success(Unit)
            }
        }
        val resolver = resolver(loader)
        val args = arrayOf<Any?>(null, "asset-abc", "/cells/path/doc.pdf", null, 0L, Dispatchers.Default)

        // First call — triggers download
        resolver.resolve(null, "asset-abc", "/cells/path/doc.pdf", null, 0L, Dispatchers.Default)
        // Second call — should use cache
        val result = resolver.resolve(null, "asset-abc", "/cells/path/doc.pdf", null, 0L, Dispatchers.Default)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { loader.load(any(), any(), any(), any(), any()) }
    }

    private fun resolver(loader: PdfRemoteLoader = mockk(relaxed = true)): PdfSourceResolver {
        val context = mockk<Context>()
        every { context.cacheDir } returns File(tempDir, "cache")
        return PdfSourceResolver(context, loader)
    }

    private fun Result<File>.viewerError(): PdfViewerError? =
        (exceptionOrNull() as? PdfSourceException)?.error
}
