/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.app.Application
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
/*
 * Run tests in isolation, use basic Application class instead of initializing WireApplication.
 * It won't work with WireApplication because of Datadog - for each test new WireApplication instance is created but Datadog uses
 * singleton and initializes itself only once for the first instance of WireApplication which then crashes for other instances.
 */
@Config(application = Application::class)
class FileHelperTest {

    @get:Rule
    val tempDir: TemporaryFolder = TemporaryFolder()

    @Test
    fun `given file does not exist when finding first unique name in directory then return this name`() {
        val desired = "abc.jpg"
        val expected = "abc.jpg"

        val result = findFirstUniqueName(tempDir.root, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file already exists when finding unique name in directory then return next available name`() {
        File(tempDir.root, "abc.jpg").createNewFile()
        val desired = "abc.jpg"
        val expected = "abc (1).jpg"

        val result = findFirstUniqueName(tempDir.root, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file and its copies already exist when finding unique name in directory then return next available name`() {
        File(tempDir.root, "abc.jpg").createNewFile()
        File(tempDir.root, "abc (1).jpg").createNewFile()
        File(tempDir.root, "abc (2).jpg").createNewFile()
        val desired = "abc.jpg"
        val expected = "abc (3).jpg"

        val result = findFirstUniqueName(tempDir.root, desired)
        assertEquals(expected, result)
    }

    @Test
    fun `given file with invalid filename when finding unique name in directory then return name without disallowed characters`() {
        val desired = "\u0020ab\u0008cd\u0000ef\u001fgh\u007Fij*kl/mn:op<qr>st?uv\\wx|yz.jpg"
        val expected = "\u0020ab_cd_ef_gh_ij_kl_mn_op_qr_st_uv_wx_yz.jpg"

        val result = desired.sanitizeFilename()
        assertEquals(expected, result)
    }

    @Test
    fun `given Android greater than 9, when saving file to downloads folder, then use correct order of executions`() = runTest {
        // given
        val arrangement = Arrangement()
            .withUriMimeType("text/plain")
            .arrange()
        val downloadedFilePath = tempDir.newFile("filename.txt").toOkioPath()
        // when
        saveFileToDownloadsFolder("name", downloadedFilePath, 100L, arrangement.context)
        // then
        verifyOrder {
            arrangement.contentResolver.insert(any(), any())
            arrangement.contentResolver.copyFile(any(), any())
        }
        verify(exactly = 0) {
            arrangement.downloadManager.addCompletedDownload(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Config(sdk = [Build.VERSION_CODES.P])
    @Test
    fun `given Android 9, when saving file to downloads folder, then use correct order of executions`() = runTest {
        // given
        val arrangement = Arrangement()
            .withUriMimeType("text/plain")
            .arrange()
        val downloadedFilePath = tempDir.newFile("filename.txt").toOkioPath()
        // when
        saveFileToDownloadsFolder("name", downloadedFilePath, 100L, arrangement.context)
        // then
        verifyOrder {
            arrangement.contentResolver.copyFile(any(), any())
            arrangement.downloadManager.addCompletedDownload(any(), any(), any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            arrangement.contentResolver.insert(any(), any())
        }
    }

    @Config(sdk = [Build.VERSION_CODES.P])
    @Test
    fun `given Android 9 and null mimeType, when saving file to downloads folder, then use mimeType indicating all types`() = runTest {
        // given
        val arrangement = Arrangement()
            .withUriMimeType(null)
            .arrange()
        val downloadedFilePath = tempDir.newFile("filename.txt").toOkioPath()
        // when
        saveFileToDownloadsFolder("name", downloadedFilePath, 100L, arrangement.context)
        // then
        verify {
            arrangement.downloadManager.addCompletedDownload(any(), any(), any(), eq("*/*"), any(), any(), any())
        }
    }

    @Test
    fun `given internal file when creating share uri then stage it under cache provider directory`() {
        val sourceFile = tempDir.newFile("secret.mp4").apply {
            writeText("video payload")
        }
        val cacheDir = tempDir.newFolder("cache")
        val stagedFile = slot<File>()
        val stagedDisplayName = slot<String>()
        val arrangement = Arrangement()
            .withCacheDir(cacheDir)
            .withStagedFileCapture(stagedFile, stagedDisplayName)
            .arrange()

        arrangement.context.pathToUri(sourceFile.toOkioPath(), "visible.mp4")

        assertEquals("visible.mp4", stagedDisplayName.captured)
        assertEquals("exported", stagedFile.captured.parentFile?.name)
        assertEquals("file-provider", stagedFile.captured.parentFile?.parentFile?.name)
        assertEquals("video payload", stagedFile.captured.readText())
        assertNotEquals(sourceFile.absolutePath, stagedFile.captured.absolutePath)
    }

    @Test
    fun `given source file when creating writable attachment uri then use provider writable cache directory`() {
        val cacheDir = tempDir.newFolder("cache")
        val stagedFile = slot<File>()
        val stagedDisplayName = slot<String>()
        val arrangement = Arrangement()
            .withCacheDir(cacheDir)
            .withStagedFileCapture(stagedFile, stagedDisplayName)
            .arrange()

        getTempWritableAttachmentUri(arrangement.context, tempDir.newFile("camera.jpg").toOkioPath())

        assertEquals("camera.jpg", stagedDisplayName.captured)
        assertEquals("writable", stagedFile.captured.parentFile?.name)
        assertEquals("file-provider", stagedFile.captured.parentFile?.parentFile?.name)
    }

    inner class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var contentResolver: ContentResolver

        @MockK
        lateinit var downloadManager: DownloadManager

        @MockK
        lateinit var uri: Uri

        private var cacheDirectory: File = tempDir.root

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(Environment::class)
            mockkStatic(FileProvider::class)
            mockkStatic("com.wire.android.util.FileUtilKt")
            coEvery { context.packageName } returns "com.wire"
            coEvery { context.cacheDir } returns cacheDirectory
            coEvery { context.contentResolver } returns contentResolver
            coEvery { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager
            coEvery { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) } returns tempDir.root
            coEvery { FileProvider.getUriForFile(any(), any(), any()) } returns uri
            coEvery { FileProvider.getUriForFile(any(), any(), any(), any()) } returns uri
            coEvery { contentResolver.insert(any(), any()) } returns uri
            coEvery { downloadManager.addCompletedDownload(any(), any(), any(), any(), any(), any(), any()) } returns 1L
            coEvery { contentResolver.copyFile(any(), any()) } returns Unit
            withUriMimeType(null)
        }

        fun withUriMimeType(mimeType: String?) = apply {
            coEvery { uri.getMimeType(context) } returns mimeType
        }

        fun withCacheDir(cacheDir: File) = apply {
            cacheDirectory = cacheDir
            coEvery { context.cacheDir } returns cacheDirectory
        }

        fun withStagedFileCapture(fileSlot: io.mockk.CapturingSlot<File>, displayNameSlot: io.mockk.CapturingSlot<String>) = apply {
            coEvery { FileProvider.getUriForFile(any(), any(), capture(fileSlot), capture(displayNameSlot)) } returns uri
        }

        fun arrange() = this
    }
}
