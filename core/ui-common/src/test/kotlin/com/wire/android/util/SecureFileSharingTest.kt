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

package com.wire.android.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SecureFileSharingTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @AfterEach
    fun tearDown() {
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun givenFileOutsideSharedCache_whenCreatingShareableUri_thenProviderReceivesDedicatedCacheFile() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply(File::mkdirs)
        val sourceFile = temporaryDirectory.resolve("external/cell.txt").toFile().apply {
            parentFile?.mkdirs()
            writeText(FILE_CONTENT)
        }
        val context = mockk<Context> {
            every { cacheDir } returns cacheDirectory
            every { packageName } returns PACKAGE_NAME
        }
        val expectedUri = mockk<Uri>()
        val providerFile = slot<File>()
        mockkStatic(FileProvider::class)
        every {
            FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, capture(providerFile), DISPLAY_NAME)
        } returns expectedUri

        val result = context.shareableFileProviderUri(sourceFile, DISPLAY_NAME)

        val sharedDirectory = File(cacheDirectory, FILE_PROVIDER_SHARED_CACHE_DIRECTORY)
        assertEquals(expectedUri, result)
        assertTrue(providerFile.captured.canonicalFile.toPath().startsWith(sharedDirectory.canonicalFile.toPath()))
        assertEquals(FILE_CONTENT, providerFile.captured.readText())
    }

    @Test
    fun givenTraversalCharactersInDisplayName_whenCreatingShareableUri_thenCacheFileStaysInsideDedicatedDirectory() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply(File::mkdirs)
        val sourceFile = temporaryDirectory.resolve("external/cell.txt").toFile().apply {
            parentFile?.mkdirs()
            writeText(FILE_CONTENT)
        }
        val context = mockk<Context> {
            every { cacheDir } returns cacheDirectory
            every { packageName } returns PACKAGE_NAME
        }
        val providerFile = slot<File>()
        mockkStatic(FileProvider::class)
        every {
            FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, capture(providerFile), TRAVERSAL_DISPLAY_NAME)
        } returns mockk()

        context.shareableFileProviderUri(sourceFile, TRAVERSAL_DISPLAY_NAME)

        val sharedDirectory = File(cacheDirectory, FILE_PROVIDER_SHARED_CACHE_DIRECTORY)
        assertTrue(providerFile.captured.canonicalFile.toPath().startsWith(sharedDirectory.canonicalFile.toPath()))
        assertEquals(".._.._cell.txt", providerFile.captured.name)
    }

    @Test
    fun givenSharedCacheUri_whenDeletingSharedCacheFile_thenFileIsRemovedFromDisk() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply(File::mkdirs)
        val sharedDirectory = File(cacheDirectory, FILE_PROVIDER_SHARED_CACHE_DIRECTORY).apply(File::mkdirs)
        val sharedFile = File(sharedDirectory, "capture.jpg").apply { writeText(FILE_CONTENT) }
        val otherFile = File(sharedDirectory, "other.jpg").apply { writeText(FILE_CONTENT) }
        val context = mockk<Context> {
            every { this@mockk.cacheDir } returns cacheDirectory
            every { packageName } returns PACKAGE_NAME
        }

        context.deleteFileProviderSharedCacheFile(sharedUri("capture.jpg"))

        assertFalse(sharedFile.exists())
        assertTrue(otherFile.exists())
    }

    @Test
    fun givenUriWithEncodedTraversal_whenDeletingSharedCacheFile_thenFileOutsideSharedCacheIsKept() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply(File::mkdirs)
        File(cacheDirectory, FILE_PROVIDER_SHARED_CACHE_DIRECTORY).mkdirs()
        val victim = temporaryDirectory.resolve("files/x").toFile().apply {
            parentFile?.mkdirs()
            writeText(FILE_CONTENT)
        }
        val context = mockk<Context> {
            every { this@mockk.cacheDir } returns cacheDirectory
            every { packageName } returns PACKAGE_NAME
        }

        context.deleteFileProviderSharedCacheFile(sharedUri("../../files/x"))

        assertTrue(victim.exists())
    }

    @Test
    fun givenFilesInSharedCache_whenDeletingSharedCacheDirectory_thenAllSharedFilesAreRemoved() {
        val cacheDirectory = temporaryDirectory.resolve("cache").toFile().apply(File::mkdirs)
        val sharedDirectory = File(cacheDirectory, FILE_PROVIDER_SHARED_CACHE_DIRECTORY).apply(File::mkdirs)
        File(sharedDirectory, "image_attachment.jpg").writeText(FILE_CONTENT)
        File(sharedDirectory, "nested").apply(File::mkdirs).resolve("report.pdf").writeText(FILE_CONTENT)
        val unrelatedFile = File(cacheDirectory, "unrelated.tmp").apply { writeText(FILE_CONTENT) }
        val context = mockk<Context> {
            every { this@mockk.cacheDir } returns cacheDirectory
        }

        context.deleteFileProviderSharedCacheDirectory()

        assertEquals(emptyList<File>(), sharedDirectory.walkTopDown().filter { it.isFile }.toList())
        assertTrue(unrelatedFile.exists())
    }

    private fun sharedUri(name: String): Uri = mockk {
        every { authority } returns PROVIDER_AUTHORITY
        every { pathSegments } returns listOf(FILE_PROVIDER_SHARED_FILES_ROOT, name)
        every { lastPathSegment } returns name
    }

    private companion object {
        const val PACKAGE_NAME = "com.wire"
        const val PROVIDER_AUTHORITY = "$PACKAGE_NAME.provider"
        const val FILE_PROVIDER_SHARED_CACHE_DIRECTORY = "file-provider-shares"
        const val DISPLAY_NAME = "Cell document.txt"
        const val TRAVERSAL_DISPLAY_NAME = "../../cell.txt"
        const val FILE_CONTENT = "cell content"
    }
}
