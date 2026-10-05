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
package com.wire.android.feature.cells.ui.upload

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.cells.domain.CellUploadRequest
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DriveUploadFilePreparerTest {

    @Test
    fun `given content uri with a display name, when resolving fileName, then it is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = "report.pdf")
            .arrange()

        val result = preparer.fileName(arrangement.uri)

        assertEquals("report.pdf", result)
    }

    @Test
    fun `given content uri whose cursor is empty, when resolving fileName, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = null)
            .arrange()

        val result = preparer.fileName(arrangement.uri)

        assertNull(result)
    }

    @Test
    fun `given content uri whose query returns no cursor, when resolving fileName, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement().arrange()
        every { arrangement.uri.scheme } returns ContentResolver.SCHEME_CONTENT
        every { arrangement.contentResolver.query(arrangement.uri, null, null, null, null) } returns null

        val result = preparer.fileName(arrangement.uri)

        assertNull(result)
    }

    @Test
    fun `given content uri whose cursor has no display name column, when resolving fileName, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement().arrange()
        val cursor = mockk<Cursor>(relaxed = true)
        every { arrangement.uri.scheme } returns ContentResolver.SCHEME_CONTENT
        every { arrangement.contentResolver.query(arrangement.uri, null, null, null, null) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns -1

        val result = preparer.fileName(arrangement.uri)

        assertNull(result)
    }

    @Test
    fun `given content uri whose query throws, when resolving fileName, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement().arrange()
        every { arrangement.uri.scheme } returns ContentResolver.SCHEME_CONTENT
        every { arrangement.contentResolver.query(arrangement.uri, null, null, null, null) } throws SecurityException("no permission")

        val result = preparer.fileName(arrangement.uri)

        assertNull(result)
    }

    @Test
    fun `given non-content uri with a path, when resolving fileName, then the file name from that path is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withFileUri("/storage/emulated/0/Download/report.pdf")
            .arrange()

        val result = preparer.fileName(arrangement.uri)

        assertEquals("report.pdf", result)
    }

    @Test
    fun `given non-content uri with no path, when resolving fileName, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withFileUri(null)
            .arrange()

        val result = preparer.fileName(arrangement.uri)

        assertNull(result)
    }

    @Test
    fun `given fileName cannot be resolved, when preparing, then null is returned and the uri content is never opened`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = null)
            .arrange()

        val result = preparer.prepare(arrangement.uri, DESTINATION_FOLDER_PATH)

        assertNull(result)
        coVerify(exactly = 0) { arrangement.contentResolver.openInputStream(any()) }
    }

    @Test
    fun `given the uri content cannot be opened, when preparing, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = "report.pdf")
            .withInputStream(null)
            .arrange()

        val result = preparer.prepare(arrangement.uri, DESTINATION_FOLDER_PATH)

        assertNull(result)
    }

    @Test
    fun `given opening the uri content throws, when preparing, then null is returned`() = runTest {
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = "report.pdf")
            .withInputStreamThrowing()
            .arrange()

        val result = preparer.prepare(arrangement.uri, DESTINATION_FOLDER_PATH)

        assertNull(result)
    }

    @Test
    fun `given a readable uri, when preparing, then it is staged and a matching upload request is returned`() = runTest {
        val stagedPath = "/tmp/staged.tmp".toPath()
        val (arrangement, preparer) = Arrangement()
            .withContentUri(fileName = "report.pdf")
            .withInputStream("file content".toByteArray())
            .withStagedFile(stagedPath, sizeBytes = 1234L)
            .arrange()

        val result = preparer.prepare(arrangement.uri, DESTINATION_FOLDER_PATH)

        assertEquals(
            CellUploadRequest(
                localPath = stagedPath,
                fileName = "report.pdf",
                sizeBytes = 1234L,
                destinationFolderPath = DESTINATION_FOLDER_PATH,
            ),
            result,
        )
    }

    private class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var contentResolver: ContentResolver

        @MockK
        lateinit var kaliumFileSystem: KaliumFileSystem

        @MockK
        lateinit var uri: Uri

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { context.contentResolver } returns contentResolver
        }

        private val preparer by lazy {
            DriveUploadFilePreparer(
                context = context,
                kaliumFileSystem = kaliumFileSystem,
                dispatchers = TestDispatcherProvider(),
            )
        }

        fun withContentUri(fileName: String?) = apply {
            every { uri.scheme } returns ContentResolver.SCHEME_CONTENT
            val cursor = mockk<Cursor>(relaxed = true)
            every { contentResolver.query(uri, null, null, null, null) } returns cursor
            if (fileName != null) {
                every { cursor.moveToFirst() } returns true
                every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
                every { cursor.getString(0) } returns fileName
            } else {
                every { cursor.moveToFirst() } returns false
            }
        }

        fun withFileUri(path: String?) = apply {
            every { uri.scheme } returns ContentResolver.SCHEME_FILE
            every { uri.path } returns path
        }

        fun withInputStream(bytes: ByteArray?) = apply {
            every { contentResolver.openInputStream(uri) } returns bytes?.inputStream()
        }

        fun withInputStreamThrowing() = apply {
            every { contentResolver.openInputStream(uri) } throws SecurityException("no permission")
        }

        fun withStagedFile(path: Path, sizeBytes: Long) = apply {
            every { kaliumFileSystem.tempFilePath(any()) } returns path
            every { kaliumFileSystem.sink(path, any()) } returns mockk<Sink>(relaxed = true)
            coEvery { kaliumFileSystem.writeData(any(), any()) } returns sizeBytes
        }

        fun arrange() = this to preparer
    }

    private companion object {
        const val DESTINATION_FOLDER_PATH = "cellName/folder"
    }
}