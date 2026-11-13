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

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContextWrapper
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.TestDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FileManagerTest {

    private val fileManager = FileManager(ApplicationProvider.getApplicationContext())

    @Test
    fun givenContentUri_whenCheckingScheme_thenItIsAccepted() {
        fileManager.checkValidSchema("content://example.provider/assets/1".toUri())
    }

    @Test
    fun givenUnsupportedUriScheme_whenCheckingScheme_thenItIsRejected() {
        val unsupportedUris = listOf(
            "file:///data/asset.txt",
            "https://example.com/asset.txt",
            "data:text/plain,asset",
            "android.resource://com.example/raw/asset",
            "custom://example/asset",
            "asset-without-scheme.txt",
        )

        unsupportedUris.forEach { uri ->
            assertThrows("Expected $uri to be rejected", IllegalArgumentException::class.java) {
                fileManager.checkValidSchema(uri.toUri())
            }
        }
    }

    @Test
    fun givenMediaStoreRejectsInsert_whenSavingToExternalStorage_thenOnErrorIsCalledInsteadOfCrashing() = runTest {
        val contentResolver = mockk<ContentResolver> {
            every { insert(any(), any()) } throws IllegalStateException("Failed to build unique file")
        }
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getContentResolver(): ContentResolver = contentResolver
        }
        var isErrorReported = false
        var isFileSavedReported = false

        FileManager(context).saveToExternalStorage(
            assetName = "image.jpg",
            assetDataPath = "/tmp/image.jpg".toPath(),
            assetDataSize = 1L,
            dispatcher = TestDispatcherProvider(),
            onError = { isErrorReported = true },
        ) { isFileSavedReported = true }

        assertTrue(isErrorReported)
        assertFalse(isFileSavedReported)
    }

    @Test
    fun givenMediaStoreRejectsFirstInsert_whenSavingToExternalStorage_thenItRetriesWithAnotherNameAndSucceeds() = runTest {
        val sourceFile = File.createTempFile("asset", ".jpg").apply { writeText("data") }
        val insertedUri = Uri.parse("content://media/external/downloads/1")
        val insertedNames = mutableListOf<String?>()
        val contentResolver = mockk<ContentResolver>(relaxed = true) {
            every { insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, any()) } answers {
                insertedNames += secondArg<ContentValues>().getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
                if (insertedNames.size == 1) throw IllegalStateException("Failed to build unique file") else insertedUri
            }
            every { openOutputStream(insertedUri) } returns ByteArrayOutputStream()
        }
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getContentResolver(): ContentResolver = contentResolver
        }
        var isErrorReported = false
        var isFileSavedReported = false

        FileManager(context).saveToExternalStorage(
            assetName = "image.jpg",
            assetDataPath = sourceFile.toOkioPath(),
            assetDataSize = sourceFile.length(),
            dispatcher = TestDispatcherProvider(),
            onError = { isErrorReported = true },
        ) { isFileSavedReported = true }

        assertFalse(isErrorReported)
        assertTrue(isFileSavedReported)
        assertEquals(2, insertedNames.size)
        assertNotEquals(insertedNames[0], insertedNames[1])
    }
}
