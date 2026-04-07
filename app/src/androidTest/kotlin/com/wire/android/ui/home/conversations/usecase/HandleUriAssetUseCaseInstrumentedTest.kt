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
package com.wire.android.ui.home.conversations.usecase

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.util.FileManager
import com.wire.android.util.getProviderAuthority
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import okio.Path.Companion.toOkioPath

@RunWith(AndroidJUnit4::class)
class HandleUriAssetUseCaseInstrumentedTest {

    private lateinit var context: Context
    private lateinit var fileManager: FileManager
    private lateinit var internalFilesFile: File
    private lateinit var internalCacheFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fileManager = FileManager(context)
        internalFilesFile = File(context.filesDir, TEST_FILES_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("private files payload")
        }
        internalCacheFile = File(context.cacheDir, TEST_CACHE_FILE_NAME).apply {
            parentFile?.mkdirs()
            writeText("private cache payload")
        }
    }

    @After
    fun tearDown() {
        if (::internalFilesFile.isInitialized) {
            internalFilesFile.delete()
        }
        if (::internalCacheFile.isInitialized) {
            internalCacheFile.delete()
        }
    }

    @Test
    fun givenShareIntentWithInternalFilesProviderUri_whenHandlingSharedAsset_thenItIsNotExposed() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.getProviderAuthority(), internalFilesFile)
        }
    }

    @Test
    fun givenShareIntentWithInternalCacheProviderUri_whenHandlingSharedAsset_thenItIsNotExposed() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.getProviderAuthority(), internalCacheFile)
        }
    }

    @Test
    fun givenShareIntentWithDatabaseFileUri_whenHandlingSharedAsset_thenItIsRejected() = runTest {
        val internalDatabaseFile = context.getDatabasePath(TEST_DATABASE_NAME).apply {
            parentFile?.mkdirs()
            writeText("top secret database contents")
        }

        val result = fileManager.getAssetBundleFromUri(Uri.fromFile(internalDatabaseFile), destinationPath(TEST_OUTPUT_DATABASE_FILE_NAME))

        assertTrue(result == null)
        internalDatabaseFile.delete()
    }

    private fun destinationPath(fileName: String) = File(context.cacheDir, "sharing-intent-test-output/$fileName")
        .apply { parentFile?.mkdirs() }
        .toOkioPath()

    companion object {
        private const val TEST_DATABASE_NAME = "share-intent-sqlcipher-test.db"
        private const val TEST_FILES_FILE_NAME = "sharing-intent/private-files.txt"
        private const val TEST_CACHE_FILE_NAME = "sharing-intent/private-cache.txt"
        private const val TEST_OUTPUT_DATABASE_FILE_NAME = "copied-database.txt"
    }
}
