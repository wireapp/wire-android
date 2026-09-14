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
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = document.absolutePath,
                assetId = null,
                remotePath = null,
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(document, result.getOrNull())
        coVerify(exactly = 0) { loader.load(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenNoLocalFileAndNoAssetInfo_whenResolving_thenItFailsAsNotFound() = runTest {
        val resolver = resolver()

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = null,
                remotePath = null,
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAnEmptyLocalFileAndNoAssetInfo_whenResolving_thenItFailsAsNotFound() = runTest {
        val empty = File(tempDir, "empty.pdf").apply { createNewFile() }
        val resolver = resolver()

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = empty.absolutePath,
                assetId = null,
                remotePath = null,
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
    }

    @Test
    fun givenAssetIdAndRemotePath_whenLoaderFails_thenItFailsAsDownloadFailed() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } returns Result.failure(Exception("network error"))
        }
        val resolver = resolver(loader)

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-123",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 1024L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    @Test
    fun givenAnAssetIdButNoRemotePath_whenResolving_thenTheKeyIsLeftToTheDownloader() = runTest {
        val loader = writingLoader(bytes = 8)
        val resolver = resolver(loader)

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-123",
                remotePath = null,
                conversationId = "conv-42",
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        // A caller without an authoritative path passes none, so DownloadCellFileUseCase resolves
        // the object key from the attachments DB instead of being handed a stale one.
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { loader.load(any(), null, any(), any(), any()) }
        assertEquals(File(File(tempDir, "files"), "conv-42/doc.pdf"), result.getOrNull())
    }

    @Test
    fun givenAMissingLocalPathAndValidAssetInfo_whenResolving_thenTheDownloadPathIsUsed() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } returns Result.failure(Exception("network error"))
        }
        val resolver = resolver(loader)

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = File(tempDir, "gone.pdf").absolutePath,
                assetId = "asset-123",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertTrue(result.isFailure)
        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
    }

    @Test
    fun givenSuccessfulDownload_whenTheFileAlreadyExists_thenLoaderIsNotCalledAgain() = runTest {
        val loader = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } coAnswers {
                val outFile = arg<File>(4)
                outFile.writeText("%PDF-1.4")
                Result.success(Unit)
            }
        }
        val resolver = resolver(loader)

        // First call — triggers download
        resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-abc",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )
        // Second call — should use cache
        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-abc",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { loader.load(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenADownload_whenResolving_thenTheLoaderWritesStraightToTheFinalPath() = runTest {
        val loader = writingLoader(bytes = 4096)
        val resolver = resolver(loader)

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-full",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 4096L,
            ),
            dispatcher = Dispatchers.Default,
        )

        // The path handed to the loader is recorded in the attachments DB by
        // DownloadCellFileUseCase, so it must be the final one -- no staging, no rename.
        val outFile = slot<File>()
        coVerify { loader.load(any(), any(), any(), any(), capture(outFile)) }
        assertEquals(outFile.captured, result.getOrNull())
        assertTrue(result.isSuccess)
        assertTrue(outFile.captured.name.endsWith(".pdf"), "was ${outFile.captured.name}")
        assertEquals(4096L, result.getOrNull()?.length())
    }

    @Test
    fun givenAFailedDownload_whenResolving_thenAnyPreviouslyDownloadedCopyIsLeftAlone() = runTest {
        val good = writingLoader(bytes = 4096)
        val cached = resolver(good)
            .resolve(
                PdfDocumentSource(
                    localPath = null,
                    assetId = "asset-keep",
                    remotePath = "/cells/path/doc.pdf",
                    conversationId = null,
                    fileName = "doc.pdf",
                    assetSize = 0L,
                ),
                dispatcher = Dispatchers.Default,
            )
            .getOrNull()

        val failing = mockk<PdfRemoteLoader> {
            coEvery { load(any(), any(), any(), any(), any()) } returns Result.failure(Exception("offline"))
        }
        val result = resolver(failing)
            .resolve(
                PdfDocumentSource(
                    localPath = null,
                    assetId = "asset-keep",
                    remotePath = "/cells/path/doc.pdf",
                    conversationId = null,
                    fileName = "doc.pdf",
                    assetSize = 0L,
                ),
                forceRefresh = true,
                dispatcher = Dispatchers.Default,
            )

        // Deleting it would strand the path DownloadCellFileUseCase already wrote to the DB.
        assertEquals(PdfViewerError.DOWNLOAD_FAILED, result.viewerError())
        assertTrue(cached?.exists() == true)
    }

    @Test
    fun givenAnAlreadyDownloadedAsset_whenForcingARefresh_thenItIsDownloadedAgain() = runTest {
        val loader = writingLoader(bytes = 8)
        val resolver = resolver(loader)

        resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-abc",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )
        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-abc",
                remotePath = "/cells/path/doc.pdf",
                conversationId = null,
                fileName = "doc.pdf",
                assetSize = 0L,
            ),
            forceRefresh = true,
            dispatcher = Dispatchers.Default,
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { loader.load(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenANodeInSubFolders_whenDownloading_thenTheRemoteStructureIsRecreated() = runTest {
        val resolver = resolver(writingLoader(bytes = 8))

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-1",
                remotePath = "conv-42/reports/q3/report.pdf",
                conversationId = "conv-42",
                fileName = "report.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        val file = result.getOrNull()
        assertEquals(File(File(tempDir, "files"), "conv-42/reports/q3/report.pdf"), file)
        assertTrue(file?.parentFile?.isDirectory == true, "sub folders should have been created")
    }

    @Test
    fun givenTwoSameNamedNodesInDifferentFolders_whenDownloading_thenTheyDoNotCollide() = runTest {
        val resolver = resolver(writingLoader(bytes = 8))

        val first = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-1",
                remotePath = "conv-42/a/report.pdf",
                conversationId = "conv-42",
                fileName = "report.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )
        val second = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-2",
                remotePath = "conv-42/b/report.pdf",
                conversationId = "conv-42",
                fileName = "report.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertNotEquals(first.getOrNull(), second.getOrNull())
    }

    @Test
    fun givenARemotePathWithoutFolders_whenDownloading_thenItIsGroupedByConversation() = runTest {
        val resolver = resolver(writingLoader(bytes = 8))

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-1",
                remotePath = "/report.pdf",
                conversationId = "conv-42",
                fileName = "report.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(File(File(tempDir, "files"), "conv-42/report.pdf"), result.getOrNull())
    }

    @Test
    fun givenARemotePathThatClimbsOutOfTheRoot_whenDownloading_thenItIsRejected() = runTest {
        val loader = writingLoader(bytes = 8)
        val resolver = resolver(loader)

        // remotePath is backend supplied, so it is never trusted as a path.
        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-1",
                remotePath = "../../../evil.pdf",
                conversationId = "conv-42",
                fileName = "evil.pdf",
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(PdfViewerError.FILE_NOT_FOUND, result.viewerError())
        coVerify(exactly = 0) { loader.load(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenNoFileNameAndNoRemotePath_whenDownloading_thenTheAssetIdIsUsed() = runTest {
        val resolver = resolver(writingLoader(bytes = 8))

        val result = resolver.resolve(
            PdfDocumentSource(
                localPath = null,
                assetId = "asset-1",
                remotePath = "",
                conversationId = "conv-42",
                fileName = null,
                assetSize = 0L,
            ),
            dispatcher = Dispatchers.Default,
        )

        assertEquals(File(File(tempDir, "files"), "conv-42/asset-1.pdf"), result.getOrNull())
    }

    private fun writingLoader(bytes: Int): PdfRemoteLoader = mockk {
        coEvery { load(any(), any(), any(), any(), any()) } coAnswers {
            arg<File>(4).writeBytes(ByteArray(bytes))
            Result.success(Unit)
        }
    }

    private fun resolver(loader: PdfRemoteLoader = mockk(relaxed = true)): PdfSourceResolver {
        val context = mockk<Context>()
        every { context.getExternalFilesDir(any()) } returns File(tempDir, "files")
        return PdfSourceResolver(context, loader)
    }

    private fun Result<File>.viewerError(): PdfViewerError? =
        (exceptionOrNull() as? PdfSourceException)?.error
}
